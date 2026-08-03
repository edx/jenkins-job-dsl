package analytics

import static org.edx.jenkins.dsl.AnalyticsConstants.common_authorization
import static org.edx.jenkins.dsl.AnalyticsConstants.common_log_rotator

class UserRetirementHealthCheck {
    public static def job = { dslFactory, allVars ->
        def gitCredentialId = allVars.get('SECURE_GIT_CREDENTIALS', '1')
        def healthCheckPasswordCredentialId = allVars.get(
            'RETIREMENT_HEALTH_CHECK_PASSWORD_CREDENTIAL_ID',
            'retirement-health-check-password'
        )
        allVars.get('DEPLOYMENTS').each { deployment, configuration ->
            configuration.get('environments').each { environment ->
                if (environment == 'stage') {

                    def environmentDeployment = "${environment}-${deployment}"
                    def jobName = "user-retirement-health-check-${deployment}-${environment}"

                    dslFactory.job(jobName) {
                        description('Run a stage-only synthetic user retirement health check and alert on regressions.')

                        authorization common_authorization(allVars)
                        concurrentBuild(false)

                        triggers {
                            cron(allVars.get('USER_RETIREMENT_HEALTH_CHECK_CRON', 'H */6 * * *'))
                        }

                        logRotator common_log_rotator(allVars)

                        wrappers {
                            timeout {
                                absolute(allVars.get('USER_RETIREMENT_HEALTH_CHECK_JOB_TIMEOUT_MINUTES', 90) as int)
                                failBuild()
                            }
                            buildUserVars()
                            buildName('#${BUILD_NUMBER}, ${ENV,var="ENVIRONMENT"}')
                            timestamps()
                            colorizeOutput('xterm')
                            credentialsBinding {
                                usernamePassword('GITHUB_USER', 'GITHUB_TOKEN', 'GITHUB_USER_PASS_COMBO')
                                string('RETIREMENT_HEALTH_CHECK_PASSWORD', healthCheckPasswordCredentialId)
                            }
                        }

                        parameters {
                            stringParam('CONFIGURATION_REPO', allVars.get('USER_RETIREMENT_HEALTH_CHECK_CONFIGURATION_REPO', 'git@github.com:edx/configuration.git'), 'Repo URL for edx/configuration.')
                            stringParam('CONFIGURATION_BRANCH', allVars.get('USER_RETIREMENT_HEALTH_CHECK_CONFIGURATION_BRANCH', 'master'), 'Repo branch for edx/configuration.')
                            stringParam('PLAYWRIGHT_E2E_REPO', allVars.get('USER_RETIREMENT_HEALTH_CHECK_PLAYWRIGHT_E2E_REPO', 'git@github.com:edx/playwright-e2e.git'), 'Repo URL for the playwright-e2e tests.')
                            stringParam('PLAYWRIGHT_E2E_BRANCH', allVars.get('USER_RETIREMENT_HEALTH_CHECK_PLAYWRIGHT_E2E_BRANCH', 'master'), 'Repo branch for the playwright-e2e tests.')
                            stringParam('TUBULAR_REPO', allVars.get('USER_RETIREMENT_HEALTH_CHECK_TUBULAR_REPO', 'git@github.com:edx/tubular.git'), 'Repo URL for the tubular scripts.')
                            stringParam('TUBULAR_BRANCH', allVars.get('USER_RETIREMENT_HEALTH_CHECK_TUBULAR_BRANCH', 'master'), 'Repo branch for the tubular scripts.')
                            stringParam('ENVIRONMENT', environmentDeployment, 'edx environment for the health check. MVP is stage-only.')
                            stringParam('RETIREMENT_JOBS_MAILING_LIST', allVars.get('RETIREMENT_JOBS_MAILING_LIST'), 'Space separated list of emails to send notifications to.')
                            stringParam('PYTHON_VERSION', allVars.get('USER_RETIREMENT_HEALTH_CHECK_PYTHON_VERSION', '3.9'), 'Python version to use for the health check virtualenv.')
                            stringParam('TIMEOUT_SECONDS', allVars.get('USER_RETIREMENT_HEALTH_CHECK_TIMEOUT_SECONDS', 1800).toString(), 'Polling timeout in seconds.')
                            stringParam('POLL_INTERVAL_SECONDS', allVars.get('USER_RETIREMENT_HEALTH_CHECK_POLL_INTERVAL_SECONDS', 30).toString(), 'Polling interval in seconds.')
                        }

                        checkoutRetryCount(5)

                        multiscm {
                            git {
                                remote {
                                    url('$CONFIGURATION_REPO')
                                    branch('$CONFIGURATION_BRANCH')
                                    credentials(gitCredentialId)
                                }
                                extensions {
                                    relativeTargetDirectory('configuration')
                                    cloneOptions {
                                        shallow()
                                        timeout(10)
                                    }
                                    cleanBeforeCheckout()
                                }
                            }
                            git {
                                remote {
                                    url('$PLAYWRIGHT_E2E_REPO')
                                    branch('$PLAYWRIGHT_E2E_BRANCH')
                                    credentials(gitCredentialId)
                                }
                                extensions {
                                    relativeTargetDirectory('playwright-e2e')
                                    cloneOptions {
                                        shallow()
                                        timeout(10)
                                    }
                                    cleanBeforeCheckout()
                                }
                            }
                            git {
                                remote {
                                    url('$TUBULAR_REPO')
                                    branch('$TUBULAR_BRANCH')
                                    credentials(gitCredentialId)
                                }
                                extensions {
                                    relativeTargetDirectory('tubular')
                                    cloneOptions {
                                        shallow()
                                        timeout(10)
                                    }
                                    cleanBeforeCheckout()
                                }
                            }
                        }

                        environmentVariables {
                            env('TEST_ENV', allVars.get('USER_RETIREMENT_HEALTH_CHECK_TEST_ENV', environment))
                            env('RUN_HEALTH_CHECK', allVars.get('USER_RETIREMENT_HEALTH_CHECK_RUN_HEALTH_CHECK', 'true'))
                            env('RETIREMENT_ARTIFACT_PATH', allVars.get('USER_RETIREMENT_HEALTH_CHECK_RETIREMENT_ARTIFACT_PATH', '${WORKSPACE}/playwright-e2e/retirement-health-check/artifact.json'))
                            env('RETIREMENT_CONFIG_FILE', allVars.get('USER_RETIREMENT_HEALTH_CHECK_RETIREMENT_CONFIG_FILE', 'stage-retirement.yml'))
                            env('TUBULAR_PATH', '${WORKSPACE}/tubular')
                            env('PYTHONIOENCODING', 'UTF-8')
                            env('LC_CTYPE', 'en_US.UTF-8')
                        }

                        steps {
                            shell(dslFactory.readFileFromWorkspace('dataeng/resources/user-retirement-health-check.sh'))
                        }

                        publishers {
                            archiveArtifacts {
                                pattern('playwright-e2e/retirement-health-check/artifact.json,playwright-e2e/playwright-report/**,playwright-e2e/test-results/**')
                                allowEmpty(true)
                            }
                            wsCleanup()

                            extendedEmail {
                                recipientList(allVars.get('RETIREMENT_JOBS_MAILING_LIST'))
                                triggers {
                                    failure {
                                        attachBuildLog(false)  // build log may contain learner data.
                                        compressBuildLog(false)
                                        subject('Build failed in Jenkins: ' + jobName + ' #${BUILD_NUMBER}')
                                        content('Build #${BUILD_NUMBER} failed.\n\nSee ${BUILD_URL} for details.\n\nRunbook: ' + allVars.get('USER_RETIREMENT_HEALTH_CHECK_RUNBOOK_URL', 'https://2u-internal.atlassian.net/wiki/spaces/AT/pages/2765422605/Django+Management+Command+Failure+Runbook'))
                                        contentType('text/plain')
                                        sendTo {
                                            recipientList()
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

package analytics

import static org.edx.jenkins.dsl.AnalyticsConstants.common_authorization
import static org.edx.jenkins.dsl.AnalyticsConstants.common_log_rotator

class UserRetirementHealthCheck {
    public static def job = { dslFactory, allVars ->
        allVars.get('HEALTH_CHECK_VARS', []).each { healthCheckVars ->
            def environmentDeployment = healthCheckVars.get('ENVIRONMENT_DEPLOYMENT')
            def jobName = 'user-retirement-health-check-' + environmentDeployment

            dslFactory.job(jobName) {
                description('Run a stage-only synthetic user retirement health check and alert on regressions.')

                authorization common_authorization(allVars)
                concurrentBuild(false)

                if (healthCheckVars.containsKey('CRON')) {
                    triggers {
                        cron(healthCheckVars.get('CRON'))
                    }
                }

                logRotator common_log_rotator(allVars)

                wrappers {
                    timeout {
                        absolute(healthCheckVars.get('JOB_TIMEOUT_MINUTES', 90) as int)
                        failBuild()
                    }
                    buildUserVars()
                    buildName('#${BUILD_NUMBER}, ${ENV,var="ENVIRONMENT"}')
                    timestamps()
                    colorizeOutput('xterm')
                    credentialsBinding {
                        usernamePassword('GITHUB_USER', 'GITHUB_TOKEN', 'GITHUB_USER_PASS_COMBO')
                    }
                }

                parameters {
                    stringParam('PLAYWRIGHT_E2E_BRANCH', healthCheckVars.get('PLAYWRIGHT_E2E_BRANCH', 'master'), 'Repo branch for the playwright-e2e tests.')
                    stringParam('TUBULAR_BRANCH', healthCheckVars.get('TUBULAR_BRANCH', 'master'), 'Repo branch for the tubular scripts.')
                    stringParam('ENVIRONMENT', environmentDeployment, 'edx environment for the health check. MVP is stage-only.')
                    stringParam('RETIREMENT_JOBS_MAILING_LIST', allVars.get('RETIREMENT_JOBS_MAILING_LIST'), 'Space separated list of emails to send notifications to.')
                    passwordParam('RETIREMENT_HEALTH_CHECK_PASSWORD', '', 'Password for the synthetic health-check user.')
                }

                checkoutRetryCount(5)

                multiscm {
                    git {
                        remote {
                            url(healthCheckVars.get('CONFIGURATION_REPO', 'https://github.com/edx/configuration.git'))
                        }
                        branch(healthCheckVars.get('CONFIGURATION_BRANCH', 'master'))
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
                            url(healthCheckVars.get('PLAYWRIGHT_E2E_REPO', 'https://github.com/edx/playwright-e2e.git'))
                        }
                        branch('$PLAYWRIGHT_E2E_BRANCH')
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
                            url(healthCheckVars.get('TUBULAR_REPO', 'https://github.com/edx/tubular.git'))
                        }
                        branch('$TUBULAR_BRANCH')
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
                    env('TEST_ENV', healthCheckVars.get('TEST_ENV', 'stage'))
                    env('RUN_HEALTH_CHECK', healthCheckVars.get('RUN_HEALTH_CHECK', 'true'))
                    env('RETIREMENT_ARTIFACT_PATH', '${WORKSPACE}/playwright-e2e/' + healthCheckVars.get('RETIREMENT_ARTIFACT_PATH', 'retirement-health-check/artifact.json'))
                    env('RETIREMENT_CONFIG_FILE', healthCheckVars.get('RETIREMENT_CONFIG_FILE', 'stage-retirement.yml'))
                    env('TUBULAR_PATH', '${WORKSPACE}/tubular')
                    env('PYTHON_VERSION', healthCheckVars.get('PYTHON_VERSION', '3.11'))
                    env('TIMEOUT_SECONDS', healthCheckVars.get('TIMEOUT_SECONDS', 1800).toString())
                    env('POLL_INTERVAL_SECONDS', healthCheckVars.get('POLL_INTERVAL_SECONDS', 30).toString())
                    env('PYTHONIOENCODING', 'UTF-8')
                    env('LC_CTYPE', 'en_US.UTF-8')
                }

                steps {
                    shell(dslFactory.readFileFromWorkspace('dataeng/resources/user-retirement-health-check.sh'))
                }

                publishers {
                    archiveArtifacts {
                        pattern('playwright-e2e/' + healthCheckVars.get('RETIREMENT_ARTIFACT_PATH', 'retirement-health-check/artifact.json') + ',playwright-e2e/playwright-report/**,playwright-e2e/test-results/**')
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
                                content('Build #${BUILD_NUMBER} failed.\n\nSee ${BUILD_URL} for details.\n\nRunbook: ' + healthCheckVars.get('RUNBOOK_URL', ''))
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

package analytics

import static org.edx.jenkins.dsl.AnalyticsConstants.common_authorization
import static org.edx.jenkins.dsl.AnalyticsConstants.common_log_rotator
import static org.edx.jenkins.dsl.AnalyticsConstants.common_publishers
import static org.edx.jenkins.dsl.AnalyticsConstants.common_wrappers
import static org.edx.jenkins.dsl.AnalyticsConstants.secure_scm_parameters

class RetireCertificates {
    public static def job = { dslFactory, allVars ->

        allVars.get('DEPLOYMENTS').each { deployment, configuration ->
            configuration.get('environments').each { environment ->

                dslFactory.job("retire-certificates-${deployment}-${environment}") {
                    description(
                        "Delete S3 certificate files and mark GeneratedCertificate records as deleted " +
                        "for retired users in the ${environment}-${deployment} environment."
                    )

                    authorization common_authorization(allVars)

                    concurrentBuild(false)

                    logRotator common_log_rotator(allVars)

                    wrappers {
                        buildName('#${BUILD_NUMBER}')
                        timestamps()
                        colorizeOutput('xterm')
                        credentialsBinding {
                            string('ROLE_ARN', "${deployment}-retired-users-certs")
                        }
                    }
                    wrappers common_wrappers(allVars)
                    parameters secure_scm_parameters(allVars)

                    parameters {
                        stringParam('CONFIGURATION_REPO', 'https://github.com/edx/configuration.git', 'Repo URL for edx/configuration.')
                        stringParam('CONFIGURATION_BRANCH', 'master', 'Repo branch for edx/configuration.')
                    }

                    environmentVariables {
                        env('ENVIRONMENT', environment)
                        env('DEPLOYMENT', deployment)
                        env('AWS_DEFAULT_REGION', allVars.get('REGION'))
                    }

                    checkoutRetryCount(5)

                    multiscm {
                        git {
                            remote {
                                url('$CONFIGURATION_REPO')
                                branch('$CONFIGURATION_BRANCH')
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
                    }

                    steps {
                        shell(dslFactory.readFileFromWorkspace('devops/resources/check_retire_users.sh'))
                    }

                    publishers common_publishers(allVars)
                }
            }
        }
    }
}

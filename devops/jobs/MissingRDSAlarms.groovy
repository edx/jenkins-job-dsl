package devops.jobs
import static org.edx.jenkins.dsl.Constants.common_wrappers

class  MissingRDSAlarms {
    public static def job = { dslFactory, extraVars ->
        assert extraVars.containsKey("DEPLOYMENTS") : "Please define DEPLOYMENTS. It should be list of strings."
        assert !(extraVars.get("DEPLOYMENTS") instanceof String) : "Make sure DEPLOYMENTS is a list of string"

        extraVars.get('DEPLOYMENTS').each { deployment, configuration ->
            configuration.environments.each { environment, rds_config ->


                dslFactory.job(extraVars.get("FOLDER_NAME","Monitoring") + "/missing-rds-alarms-${deployment}") {
                    parameters {
                        stringParam('CONFIGURATION_REPO', 'https://github.com/edx/configuration.git')
                        stringParam('CONFIGURATION_BRANCH', 'master')
                    }

                    wrappers common_wrappers

                    wrappers {
                        credentialsBinding {
                            file("AWS_CONFIG_FILE","tools-edx-jenkins-aws-credentials")
                            def variable = "${deployment}-missing-rds-alarms"
                            string("ROLE_ARN", variable)
                        }
                    }

                    triggers {
                        cron("H H * * *")
                    }

                    def ignore_options = ""
                    rds_config.rds.each { db ->
                        ignore_options = "${ignore_options}--ignore ${db} "
                    }

                    environmentVariables {
                        env('AWS_DEFAULT_REGION', extraVars.get('REGION'))
                        env('WHITELIST', whitelist)
                    }

                    multiscm {
                        git {
                            remote {
                                url('$CONFIGURATION_REPO')
                                branch('$CONFIGURATION_BRANCH')
                            }
                            extensions {
                                cleanAfterCheckout()
                                pruneBranches()
                                relativeTargetDirectory('configuration')
                            }
                        }
                    }
                    steps {
                        virtualenv {
                            pythonName('System-CPython-3.6')
                            nature("shell")
                            systemSitePackages(false)

                            command(
                                    dslFactory.readFileFromWorkspace("devops/resources/missing-rds-alarms.sh")
                            )
                        }
                    }
                   publishers {
                        extendedEmail {
                            recipientList(extraVars.get('NOTIFY_ON_FAILURE'))
                            triggers {
                                  failure {
                                     attachBuildLog(false)  // build log contains PII!
                                     compressBuildLog(false)  // build log contains PII!
                                     subject('Failed build: ${JOB_NAME} #${BUILD_NUMBER}')
                                     content('Jenkins job: ${JOB_NAME} failed. \nFor' + " ${deployment} " + 'Environment. \n\nSee ${BUILD_URL} for details.')
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

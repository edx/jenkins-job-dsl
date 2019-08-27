package devops.jobs
import static org.edx.jenkins.dsl.Constants.common_wrappers
import static org.edx.jenkins.dsl.Constants.common_logrotator

class ExportRDSDeadLocks {
    public static def job = { dslFactory, extraVars ->
        assert extraVars.containsKey("DEPLOYMENTS") : "Please define DEPLOYMENTS. It should be list of strings."
        assert extraVars.containsKey("IGNORE_LIST") : "Please define IGNORE_LIST. It should be list of strings."
        assert !(extraVars.get("DEPLOYMENTS") instanceof String) : "Make sure DEPLOYMENTS is a list of string"

        extraVars.get('DEPLOYMENTS').each { deployment, configuration ->
            configuration.environments.each { environment ->

                dslFactory.job(extraVars.get("FOLDER_NAME","Monitoring") + "/export-dead-locks-${deployment}-${environment}") {
                    parameters {
                        stringParam('CONFIGURATION_REPO', 'https://github.com/edx/configuration.git')
                        stringParam('CONFIGURATION_BRANCH', 'master')
                    }

                    wrappers common_wrappers
                    logRotator common_logrotator

                    wrappers {
                        credentialsBinding {
                            usernamePassword("USERNAME", "PASSWORD", "${deployment}-${environment}-export-dead-locks-credentials")
                            usernamePassword("SPLUNKUSERNAME", "SPLUNKPASSWORD", "export-dead-locks-splunk-credentials")
                            file("AWS_CONFIG_FILE","tools-edx-jenkins-aws-credentials")
                            def variable = "${deployment}-export-dead-locks"
                            string("ROLE_ARN", variable)
                        }
                    }

                    triggers {
                        cron("H H * * *")
                    }

                    def INDEXNAME = "${environment}-${deployment}"

                    def rdsignore = ""
                    extraVars.get('IGNORE_LIST').each { ignore ->
                        rdsignore = "${rdsignore}-i ${ignore} "
                    }

                    environmentVariables {
                        env('AWS_DEFAULT_REGION', extraVars.get('REGION'))
                        env('ENVIRONMENT', environment)
                        env('HOSTNAME', extraVars.get('SPLUNKHOSTNAME'))
                        env('PORT', extraVars.get('PORT'))
                        env('INDEXNAME', INDEXNAME)
                        env('RDSIGNORE', rdsignore)
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
                            nature("shell")
                            systemSitePackages(false)

                            command(
                                    dslFactory.readFileFromWorkspace("devops/resources/export-dead-locks.sh")
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

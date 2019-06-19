package devops.jobs
import static org.edx.jenkins.dsl.Constants.common_wrappers

class CheckTableSize {
    public static def job = { dslFactory, extraVars ->
        assert extraVars.containsKey("DEPLOYMENTS") : "Please define DEPLOYMENTS. It should be list of strings."
        assert extraVars.containsKey("IGNORE_LIST") : "Please define IGNORE_LIST. It should be list of strings."
        assert !(extraVars.get("DEPLOYMENTS") instanceof String) : "Make sure DEPLOYMENTS is a list of string"

        extraVars.get('DEPLOYMENTS').each { deployment , configuration ->
            configuration.environments.each { environment, rds_config ->


                dslFactory.job(extraVars.get("FOLDER_NAME","Monitoring") + "/table-size-monitoring-${deployment}") {
                    parameters {
                        stringParam('CONFIGURATION_REPO', 'https://github.com/edx/configuration.git')
                        stringParam('CONFIGURATION_BRANCH', 'master')
                    }

                    wrappers common_wrappers

                    wrappers {
                        credentialsBinding {
                            usernamePassword("USERNAME", "PASSWORD", "${deployment}-table-size-credentials")
                            file("AWS_CONFIG_FILE","tools-edx-jenkins-aws-credentials")
                            def variable = "${deployment}-table-size-monitoring"
                            string("ROLE_ARN", variable)
                        }
                    }

                    triggers {
                        cron("H * * * *")
                    }

                    def rdsthreshold = ""
                    rds_config.rds.each { rds, threshold ->
                        rdsthreshold = "${rdsthreshold}--rdsthreshold ${rds} ${threshold} "
                    }

                    def rdsignore = ""
                    extraVars.get('IGNORE_LIST').each { ignore ->
                        rdsignore = "${rdsignore}-i ${ignore} "
                    }

                    environmentVariables {
                        env('AWS_DEFAULT_REGION', extraVars.get('REGION'))
                        env('THRESHOLD', extraVars.get('THRESHOLD'))
                        env('RDSTHRESHOLD', rdsthreshold)
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
                                    dslFactory.readFileFromWorkspace("devops/resources/table-size-monitoring.sh")
                            )
                        }
                    }

                    publishers {
                        mailer(extraVars.get('NOTIFY_ON_FAILURE'), false, false)

                    }
                }
           }
       }
   }
}

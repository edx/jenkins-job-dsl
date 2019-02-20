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
                        stringParam('CONFIGURATION_BRANCH', 'ihassan/OPS-3506_missing_rds_cloudwatch_alarms')
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

                    def whitelist = ""
                    rds_config.rds.each { dbs ->
                        whitelist = "${whitelist}--whitelist ${dbs} "
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
                            nature("shell")
                            systemSitePackages(false)

                            command(
                                    dslFactory.readFileFromWorkspace("devops/resources/missing-rds-alarms.sh")
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

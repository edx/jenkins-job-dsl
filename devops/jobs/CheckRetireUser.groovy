package devops.jobs
import static org.edx.jenkins.dsl.Constants.common_wrappers

class CheckRetireUsers {
    public static def job = { dslFactory, extraVars ->
        assert extraVars.containsKey("DEPLOYMENTS") : "Please define DEPLOYMENTS. It should be list of strings."
        assert extraVars.containsKey("IGNORE_LIST") : "Please define IGNORE_LIST. It should be list of strings."
        assert !(extraVars.get("DEPLOYMENTS") instanceof String) : "Make sure DEPLOYMENTS is a list of string"

        extraVars.get('DEPLOYMENTS').each { deployment , configuration ->
            configuration.environments.each { environment ->


                dslFactory.job(extraVars.get("FOLDER_NAME","Monitoring") + "/check-retire-users-for-${deployment}") {
                    parameters {
                        stringParam('CONFIGURATION_REPO', 'https://github.com/edx/configuration.git')
                        stringParam('CONFIGURATION_BRANCH', 'master')
                    }

                    wrappers common_wrappers

                    wrappers {
                        credentialsBinding {
                            usernamePassword("USERNAME", "PASSWORD", "${deployment}-users-retire-credentials")
                            def variable = "${deployment}-check-retire-users"
                            string("ROLE_ARN", variable)
                        }
                    }

                    def rdsignore = ""
                    extraVars.get('IGNORE_LIST').each { ignore ->
                        rdsignore = "${rdsignore}-i ${ignore} "
                    }

                    def whitelistregions = ""
                    configuration.REGION_LIST.each { include ->
                        whitelistregions = "${whitelistregions}-r ${include} "
                    }

                    environmentVariables {
                        env('ENVIRONMENT', environment)
                        env('DEPLOYMENT', deployment)
                        env('AWS_DEFAULT_REGION', extraVars.get('REGION'))
                        env('RDSIGNORE', rdsignore)
                        env('WHITELISTREGIONS', whitelistregions)
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
                       shell(dslFactory.readFileFromWorkspace('devops/resources/check_retire_users.sh'))

                    }

                    publishers {
                        mailer(extraVars.get('NOTIFY_ON_FAILURE'), false, false)

                    }
                }
           }
       }
   }
}

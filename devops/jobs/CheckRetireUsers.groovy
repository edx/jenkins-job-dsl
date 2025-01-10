package devops.jobs
import static org.edx.jenkins.dsl.Constants.common_wrappers

class CheckRetireUsers {
    public static def job = { dslFactory, extraVars ->
        assert extraVars.containsKey("DEPLOYMENTS") : "Please define DEPLOYMENTS. It should be list of strings."
        assert !(extraVars.get("DEPLOYMENTS") instanceof String) : "Make sure DEPLOYMENTS is a list of string"

        extraVars.get('DEPLOYMENTS').each { deployment , configuration ->
            configuration.environments.each { environment ->


                dslFactory.job(extraVars.get("FOLDER_NAME","Monitoring") + "/check-retire-users-for-${deployment}-${environment}") {
                    parameters {
                        stringParam('CONFIGURATION_REPO', 'https://github.com/edx/configuration.git')
                        stringParam('CONFIGURATION_BRANCH', 'master')
                    }

                    wrappers common_wrappers

                    wrappers {
                        credentialsBinding {
                            usernamePassword("DB_USER", "DB_PASSWORD", "${deployment}-${environment}-users-retire-credentials")
                            def variable = "${deployment}-retired-users-certs"
                            string("ROLE_ARN", variable)
                        }
                    }

                    environmentVariables {
                        env('ENVIRONMENT', environment)
                        env('DEPLOYMENT', deployment)
                        env('AWS_DEFAULT_REGION', extraVars.get('REGION'))
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

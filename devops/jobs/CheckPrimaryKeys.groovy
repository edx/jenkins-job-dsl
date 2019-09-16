package devops.jobs
import static org.edx.jenkins.dsl.Constants.common_wrappers

class CheckPrimaryKeys {
    public static def job = { dslFactory, extraVars ->
        assert extraVars.containsKey("DEPLOYMENTS") : "Please define DEPLOYMENTS. It should be list of strings."
        assert extraVars.containsKey("IGNORE_LIST") : "Please define IGNORE_LIST. It should be list of strings."
        assert !(extraVars.get("DEPLOYMENTS") instanceof String) : "Make sure DEPLOYMENTS is a list of string"

        extraVars.get('DEPLOYMENTS').each { deployment , configuration ->
            configuration.environments.each { environment ->


                dslFactory.job(extraVars.get("FOLDER_NAME","Monitoring") + "/check-primary-keys-for-${deployment}") {
                    parameters {
                        stringParam('CONFIGURATION_REPO', 'https://github.com/edx/configuration.git')
                        stringParam('CONFIGURATION_BRANCH', 'master')
                        stringParam('TO_ADDRESS', extraVars.get('TO_ADDRESS', ""))
                        stringParam('FROM_ADDRESS', extraVars.get('FROM_ADDRESS', ""))
                    }

                    wrappers common_wrappers

                    wrappers {
                        credentialsBinding {
                            usernamePassword("USERNAME", "PASSWORD", "${deployment}-table-size-credentials")
                            file("AWS_CONFIG_FILE","tools-edx-jenkins-aws-credentials")
                            def variable = "${deployment}-check-primary-keys"
                            string("ROLE_ARN", variable)
                        }
                    }

                    triggers {
                        cron("H H * * 1")
                    }

                    def rdsignore = ""
                    extraVars.get('IGNORE_LIST').each { ignore ->
                        rdsignore = "${rdsignore}-i ${ignore} "
                    }

                    environmentVariables {
                        env('ENVIRONMENT', environment)
                        env('DEPLOYMENT', deployment)
                        env('AWS_DEFAULT_REGION', extraVars.get('REGION'))
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
                            pythonName('System-CPython-3.6')
                            nature("shell")
                            systemSitePackages(false)

                            command(
                                    dslFactory.readFileFromWorkspace("devops/resources/check_primary_keys.sh")
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

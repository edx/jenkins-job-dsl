package devops.jobs
import static org.edx.jenkins.dsl.Constants.common_wrappers

class  MissingNewRelicAlerts {
    public static def job = { dslFactory, extraVars ->
        assert extraVars.containsKey("DEPLOYMENTS") : "Please define DEPLOYMENTS. It should be list of strings."
        assert !(extraVars.get("DEPLOYMENTS") instanceof String) : "Make sure DEPLOYMENTS is a list of string"

        extraVars.get('DEPLOYMENTS').each { deployment ->

                dslFactory.job(extraVars.get("FOLDER_NAME","Monitoring") + "/missing-new-relic-alerts-${deployment}") {
                    parameters {
                        stringParam('CONFIGURATION_REPO', 'https://github.com/edx/configuration.git')
                        stringParam('CONFIGURATION_BRANCH', 'master')
                    }

                    wrappers common_wrappers

                    wrappers {
                        credentialsBinding {
                            string("NEW_RELIC_API_KEY", "${deployment}-new-relic-api-key")
                            file("AWS_CONFIG_FILE","tools-edx-jenkins-aws-credentials")
                            def variable = "${deployment}-missing-newrelic-alerts"
                            string("ROLE_ARN", variable)
                        }
                    }

                    triggers {
                        cron("H 0 * * 0")
                    }

                    def ignore_list = ""
                    extraVars.get('IGNORE_LIST').each { ignore ->
                        ignore_list = "${ignore_list}-i ${ignore} "
                    }

                    environmentVariables {
                        env('AWS_DEFAULT_REGION', extraVars.get('REGION'))
                        env('IGNORE_LIST', ignore_list)
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
                                    dslFactory.readFileFromWorkspace("devops/resources/missing-newrelic-alerts.sh")
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

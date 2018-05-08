/*

    Variables without defaults are marked (required)

    Variables consumed for this job:
        * NOTIFY_ON_FAILURE: (Required - email address)
        * SECURE_GIT_CREDENTIALS: (Required - jenkins name of git credentials)
        * PINGDOM_EMAIL: (Required - The Pingdom Email)
        * PINGDOM_API_KEY: (Required - Pingdom API Key)
        * PINGDOM_PASSWORD: (Required - Pingdom Password)
        * PINGDOM_ALERT_CONFIG_FILE (The pingdom Alert config file)
        * CONFIGURATION_REPO_URL: URL of the configuration GitHub repository (REQUIRED)

*/
package devops.jobs


import static org.edx.jenkins.dsl.DevopsConstants.merge_to_master_trigger
import static org.edx.jenkins.dsl.Constants.common_wrappers
import static org.edx.jenkins.dsl.Constants.common_logrotator


class CreatePingdomAlerts {
    public static def job = { dslFactory, extraVars ->
        assert extraVars.containsKey('PINGDOM_EMAIL') : "Required PINGDOM_EMAIL setting missing from configuration"
        assert extraVars.containsKey('PINGDOM_ALERT_CONFIG_FILE') : "Required pingdom alert config file(PINGDOM_ALERT_CONFIG_FILE) missing from configuration"
        dslFactory.job(extraVars.get("FOLDER_NAME","Monitoring") + "/create_pingdom_alerts") {
            assert extraVars.containsKey('CONFIGURATION_REPO_URL') : "Please define CONFIGURATION_REPO_URL"
            wrappers common_wrappers
            logRotator common_logrotator


            wrappers {
                    credentialsBinding {
                        string('PINGDOM_API_KEY', 'PINGDOM_API_KEY')
                        string('PINGDOM_PASSWORD', 'PINGDOM_PASSWORD')
                    }
            }

            def gitCredentialId = extraVars.get('SECURE_GIT_CREDENTIALS','')


            parameters{
                stringParam('CONFIGURATION_INTERNAL_REPO', extraVars.get('CONFIGURATION_INTERNAL_REPO',  "git@github.com:edx/edx-internal.git"),
                            'Git repo containing internal overrides')
                stringParam('CONFIGURATION_INTERNAL_BRANCH', extraVars.get('CONFIGURATION_INTERNAL_BRANCH', 'master'),
                        'e.g. tagname or origin/branchname')
            }
            def config_branch = 'master'

            environmentVariables {
	        env('PINGDOM_EMAIL', extraVars.get('PINGDOM_EMAIL'))
	        env('PINGDOM_ALERT_CONFIG_FILE', extraVars.get('PINGDOM_ALERT_CONFIG_FILE'))
            }

            properties {
                githubProjectUrl(extraVars.get("CONFIGURATION_REPO_URL"))
            }

            triggers merge_to_master_trigger(config_branch)

            steps {
                virtualenv {
                    pythonName('System-CPython-3.5')
                    nature("shell")
                    systemSitePackages(false)

                    command(
                        dslFactory.readFileFromWorkspace("devops/resources/create-pingdom-alerts.sh")
                    )

                }

            }

            multiscm{
                git {
                    remote {
                        url('https://github.com/edx/configuration.git')
                        branch('master')
                    }
                    extensions {
                        cleanAfterCheckout()
                        pruneBranches()
                        relativeTargetDirectory('configuration')
                    }
                }
                git {
                    remote {
                        url('$CONFIGURATION_INTERNAL_REPO')
                        branch('$CONFIGURATION_INTERNAL_BRANCH')
                            if (gitCredentialId) {
                                credentials(gitCredentialId)
                            }
                    }
                    extensions {
                        cleanAfterCheckout()
                        pruneBranches()
                        relativeTargetDirectory('configuration-internal')
                    }
                    }
            }

            publishers {
                mailer(extraVars.get('NOTIFY_ON_FAILURE'), false, false)
            }

        }
    }
}

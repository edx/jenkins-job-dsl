/*

    Variables without defaults are marked (required)

    Variables consumed for this job:
        * NOTIFY_ON_FAILURE: (Required - email address)
        * SECURE_GIT_CREDENTIALS: (Required - jenkins name of git credentials)
        * ZONE_ID: (Required - The CloudFlare Zone ID)
        * AUTH_KEY: (Required - Authentication key for the account that would make API calls)
        * EMAIL: (Required - Email of the account for making API calls)

*/
package devops.jobs


import static org.edx.jenkins.dsl.Constants.common_wrappers
import static org.edx.jenkins.dsl.Constants.common_logrotator


class CloudFlareHitRate {
    public static def job = { dslFactory, extraVars ->
        assert extraVars.containsKey('ZONE_ID') : "Required ZONE_ID setting missing from configuration"
        assert extraVars.containsKey('EMAIL') : "Required email(EMAIL) setting missing from configuration"
        dslFactory.job(extraVars.get("FOLDER_NAME","Monitoring") + "/cloudflare-hit-rate-edx") {
            wrappers common_wrappers
            logRotator common_logrotator

            wrappers {
                    credentialsBinding {
                        string('AUTH_KEY', 'AUTH_KEY')
                    }
            }

            environmentVariables {
	        env('ZONE_ID', extraVars.get('ZONE_ID'))
	        env('EMAIL', extraVars.get('EMAIL'))
	        env('THRESHOLD', extraVars.get('THRESHOLD'))
            }

            triggers {
                cron("* */1 * * *")
            }

            steps {
                virtualenv {
                    pythonName('System-CPython-3.5')
                    nature("shell")
                    systemSitePackages(false)

                    command(
                        dslFactory.readFileFromWorkspace("devops/resources/cloudflare-hit-rate.sh")
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
            }

            publishers {
                mailer(extraVars.get('NOTIFY_ON_FAILURE'), false, false)
            }

        }
    }
}

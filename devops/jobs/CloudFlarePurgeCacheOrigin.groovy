/*

    Variables consumed from the EXTRA_VARS input to your seed job in addition
    to those listed in the seed job.

    Variables consumed for this job:
        * CLOUDFLARE_EMAIL: (email address)
        * CLOUDFLARE_API_KEY: (Required - CLoudFlare API key)
        * ORIGIN: (Required)
        * CLOUDFLARE_ZONE_ID: (Required - The CloudFlare Zone ID)
        * BUCKET: (Required - To generate target list)
        * CLOUDFLARE_SITE_URL: (Required)

*/
package devops.jobs

import static org.edx.jenkins.dsl.Constants.common_wrappers
import static org.edx.jenkins.dsl.Constants.common_logrotator

class CloudFlarePurgeCacheOrigin {
    public static def job = { dslFactory, extraVars ->
        dslFactory.job(extraVars.get("FOLDER_NAME","Monitoring") + "/cloudflare-purge-cache-origin") {
            wrappers common_wrappers
            logRotator common_logrotator


            parameters {
                stringParam("ORIGIN", extraVars.get('ORIGIN'),"origin")
                choiceParam("SITE",
                            ["https://edx-video.net",
                             "https://edx-cdn.org",
                             "https://edx.org",
                            ],
                            "Site for which you want to purge cache")
                stringParam("BUCKET", "edx-course-videos","")
            }

            wrappers {
                    credentialsBinding {
                        string('AUTH_KEY', 'AUTH_KEY')
                    }
            }
            environmentVariables {
	        env('ZONE_ID', extraVars.get('ZONE_ID'))
	        env('EMAIL', extraVars.get('EMAIL'))
            }

            def gitCredentialId = extraVars.get('SECURE_GIT_CREDENTIALS','')

            multiscm {
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
                        url('git@github.com:edx/edx-internal.git')
                        branch('master')
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

            steps {

                virtualenv {
                    nature("shell")
                    systemSitePackages(false)
                    command(dslFactory.readFileFromWorkspace("devops/resources/cloudflare_purge_cache.sh"))

                }

            }
            publishers {
                mailer(extraVars.get('NOTIFY_ON_FAILURE'), false, false)
            }


        }
    }
}

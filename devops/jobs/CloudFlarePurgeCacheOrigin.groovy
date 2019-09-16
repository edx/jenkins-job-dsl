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
                stringParam("ORIGIN", extraVars.get('ORIGIN'),
                "The origin, for example https://edx.org, https://stage.edx.org or https://cmeonline.hms.harvard.edu. Be sure you know what you're doing if you make this blank.")
                choiceParam("SITE",
                            ["https://edx-video.net",
                             "https://edx-cdn.org",
                             "https://edx.org",
                            ],
                            "The Site that you want to purge cache of. Look at the domain fields of https://github.com/edx/terraform/blob/master/cloudflare/main.tf for examples.")
                stringParam("BUCKET", "edx-course-videos",
                "The bucket that you want to purge the cache of. Look at the value fields of https://github.com/edx/terraform/blob/master/cloudflare/main.tf for examples.")
                booleanParam("CONFIRM_PURGE",false, "Check this in order to pruge cache for the site")
            }

            wrappers {
                    credentialsBinding {
                        string('AUTH_KEY', 'AUTH_KEY')
                        file("AWS_CONFIG_FILE","tools-edx-jenkins-aws-credentials")
                        def variable = "tools_jenkins_s3_cloudflare_target"
                        string("ROLE_ARN", variable)
                    }
            }

            environmentVariables {
	        env('EDX_ZONE_ID', extraVars.get('ZONE_ID'))
	        env('EDX_CDN_ZONE_ID', extraVars.get('EDX_CDN_ZONE_ID'))
	        env('EDX_VIDEO_ZONE_ID', extraVars.get('EDX_VIDEO_ZONE_ID'))
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
                    pythonName('System-CPython-3.6')
                    nature("shell")
                    systemSitePackages(false)
                    command(dslFactory.readFileFromWorkspace("devops/resources/cloudflare_purge_cache.sh"))

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
                             content('Jenkins job: ${JOB_NAME} failed.\n\nSee ${BUILD_URL} for details.')
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

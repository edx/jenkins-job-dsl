/*

    Variables without defaults are marked (required)

    Variables consumed for this job:
        * EMAIL: (Required - email address to send results to)
        * SECURE_GIT_CREDENTIALS: (Required - jenkins name of git credentials)
        * ALGOLIA_AUTH_KEY: (Required - Authentication key for Algolia API calls)
        * CONTENTFUL_AUTH_KEY: (Required - Authentication key for Contentful API calls)
*/
package engagement.jobs


import static org.edx.jenkins.dsl.Constants.common_wrappers
import static org.edx.jenkins.dsl.Constants.common_logrotator


class WebsiteVendorMonitoring {
    public static def job = { dslFactory, extraVars ->
        assert extraVars.containsKey('EMAIL') : "Required email(EMAIL) setting missing"
        dslFactory.job(extraVars.get("FOLDER_NAME", "Engagement") + "/website-vendor-monitoring") {
            wrappers common_wrappers
            logRotator common_logrotator

            wrappers {
                    credentialsBinding {
                        string('ALGOLIA_AUTH_KEY', 'ALGOLIA_AUTH_KEY')
                        string('CONTENTFUL_AUTH_KEY', 'CONTENTFUL_AUTH_KEY')
                    }
            }

            def gitCredentialId = extraVars.get('SECURE_GIT_CREDENTIALS','')

            environmentVariables {
	              env('EMAIL', extraVars.get('EMAIL'))
            }

            triggers {
                cron("H 10 * * *")
            }

            steps {
               shell(dslFactory.readFileFromWorkspace('engagement/resources/website-vendor-monitoring.sh'))
            }

            multiscm {
                git {
                    remote {
                        url('https://github.com/edx/prospectus.git')
                        branch('master')
                    }
                    extensions {
                        cleanAfterCheckout()
                        pruneBranches()
                        relativeTargetDirectory('prospectus')
                    }
                }
            }

            publishers {
                extendedEmail {
                    recipientList(extraVars.get('EMAIL'))
                    triggers {
                         failure {
                             attachBuildLog(false)
                             compressBuildLog(false)
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

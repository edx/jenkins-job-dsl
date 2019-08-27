/*
 Removes all unused resources (images, volumes, containers, networks)
 Vars consumed for this job:
    * NOTIFY_ON_FAILURE: alert@example.com
    * FOLDER_NAME: folder
*/

package devops.jobs
import static org.edx.jenkins.dsl.Constants.common_logrotator
import static org.edx.jenkins.dsl.Constants.common_wrappers

class DockerCleanup{
    public static def job = { dslFactory, extraVars ->
        dslFactory.job(extraVars.get("FOLDER_NAME","Monitoring") + "/docker-cleanup") {
            
            logRotator common_logrotator
            wrappers common_wrappers
            
            triggers{
                cron("H 0 * * *")
            }

            steps {
                shell('docker system prune -f')
            }

            if (extraVars.get('NOTIFY_ON_FAILURE')){
                publishers {
                    extendedEmail {
                        recipientList(extraVars.get('NOTIFY_ON_FAILURE'))
                        triggers {
                             failure {
                                 attachBuildLog(false)  // build log contains PII!
                                 compressBuildLog(false)  // build log contains PII!
                                 subject('Failed build: ${JOB_NAME} #${BUILD_NUMBER}')
                                 content('Jenkins job: ${JOB_NAME} failed. \nFor' + " ${deployment} " + 'Environment. \n\nSee ${BUILD_URL} for details.')
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

}

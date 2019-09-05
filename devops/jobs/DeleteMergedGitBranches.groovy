/*
 Variables without defaults are marked (required) 
 
 Variables consumed for this job:
    * SECURE_GIT_CREDENTIALS: secure-bot-user (required)
    * SSH_AGENT_KEY : ssh-credential-name
    * NOTIFY_ON_FAILURE: alert@example.com
    * FOLDER_NAME: folder, default is Monitoring
*/

package devops.jobs
import static org.edx.jenkins.dsl.Constants.common_wrappers
import static org.edx.jenkins.dsl.Constants.common_logrotator

class DeleteMergedGitBranches{
    public static def job = { dslFactory, extraVars ->
            dslFactory.job(extraVars.get("FOLDER_NAME","Monitoring") + "/delete-merged-git-branches") {
                wrappers common_wrappers
                logRotator common_logrotator
                
                def gitCredentialId = extraVars.get('SECURE_GIT_CREDENTIALS','')

                multiscm{
                    git {
                        remote {
                            url('git@github.com:edx/edx-platform.git')
                            branch('master')
                            if (gitCredentialId) {
                                credentials(gitCredentialId)
                            }
                        }
                        extensions {
                            cleanAfterCheckout()
                            pruneBranches()
                            relativeTargetDirectory('edx-platform')
                        }
                    }
                }

                triggers {
                    cron("H 15 * * 7")
                }

                steps {
                    virtualenv {
                        pythonName('System-CPython-3.5')
                        nature("shell")
                        systemSitePackages(false)

                        command(
                            dslFactory.readFileFromWorkspace("devops/resources/delete-merged-git-branches.sh")
                        )
                    }
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
                wrappers {
                    sshAgent(extraVars.get("SSH_AGENT_KEY"))
                }
           }
      }
}

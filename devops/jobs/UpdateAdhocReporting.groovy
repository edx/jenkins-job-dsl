/*
 Variables without defaults are marked (required) 
 
 Variables consumed for this job:
    * SECURE_GIT_CREDENTIALS: secure-bot-user (required)
    * SSH_AGENT_KEY : ssh-credential-name
    * NOTIFY_ON_FAILURE: alert@example.com
    * FOLDER_NAME: folder, default is Monitoring

 This job expects the following credentials to be defined on the folder
    tools-edx-jenkins-aws-credentials: file with key/secret in boto config format
    update-adhoc-reporting-role-arn: the role to aws sts assume-role
*/

package devops.jobs
import static org.edx.jenkins.dsl.Constants.common_wrappers
import static org.edx.jenkins.dsl.Constants.common_logrotator

      
class UpdateAdhocReporting{
       public static def job = { dslFactory, extraVars ->
	      
            dslFactory.job(extraVars.get("FOLDER_NAME","Monitoring") + "/update-adhoc-reporting") {
                wrappers common_wrappers
                logRotator common_logrotator

                wrappers {
                    credentialsBinding {
                        file('AWS_CONFIG_FILE','tools-edx-jenkins-aws-credentials')
                        string('ROLE_ARN', "update-adhoc-reporting-role-arn")
                    }
                    sshAgent(extraVars.get("SSH_AGENT_KEY"))
                }

                def gitCredentialId = extraVars.get('SECURE_GIT_CREDENTIALS','')

                
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
                            url('git@github.com:edx/edx-internal.git')
                            branch('master')
                            if (gitCredentialId) {
                                credentials(gitCredentialId)
                            }
                        }
                        extensions {
                            cleanAfterCheckout()
                            pruneBranches()
                            relativeTargetDirectory('edx-internal')
                        }
                    }
                    git {
                        remote {
                            url('git@github.com:edx-ops/edx-secure.git')
                            branch('master')
                            if (gitCredentialId) {
                                credentials(gitCredentialId)
                            }
                        }
                        extensions {
                            cleanAfterCheckout()
                            pruneBranches()
                            relativeTargetDirectory('edx-secure')
                        }
                    }
                    git {
                        remote {
                            url('git@github.com:edx/edge-internal.git')
                            branch('master')
                            if (gitCredentialId) {
                                credentials(gitCredentialId)
                            }
                        }
                        extensions {
                            cleanAfterCheckout()
                            pruneBranches()
                            relativeTargetDirectory('edge-internal')
                        }
                    }
                    git {
                        remote {
                            url('git@github.com:edx-ops/edge-secure.git')
                            branch('master')
                            if (gitCredentialId) {
                                credentials(gitCredentialId)
                            }
                        }
                        extensions {
                            cleanAfterCheckout()
                            pruneBranches()
                            relativeTargetDirectory('edge-secure')
                        }
                    }
                }

                triggers {
                    cron("H * * * * ")
                }

                environmentVariables {
                    env('REGION', extraVars.get('REGION','us-east-1'))
                    env('ANSIBLE_SSH_USER', extraVars.get('SSH_USER','ubuntu'))
                }


                steps {
                    virtualenv {
                        pythonName('System-CPython-3.5')
                        nature("shell")
                        systemSitePackages(false)

                        command(
                            dslFactory.readFileFromWorkspace("devops/resources/update-adhoc-reporting.sh")
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
           }
      }
}

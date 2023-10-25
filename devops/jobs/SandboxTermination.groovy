/*
 Variables without defaults are marked (required) 
 
 Variables consumed for this job:
    * NOOP: boolean value to perform no operations, default is false
    * SECURE_GIT_CREDENTIALS: secure-bot-user (required)
    * JENKINS_DSL_INTERNAL_REPO: repository containing sandbox termination python script (required)
    * JENKINS_DSL_INTERNAL_REPO_BRANCH: default is master
    * CONFIGURATION_REPO: name of config repo, default is https://github.com/edx/configuration.git
    * CONFIGURATION_BRANCH: default is master
    * CONFIGURAITON_INTERNAL_REPO: Git repo containing internal overrides, default is git@github.com:edx/edx-internal.git
    * CONFIGURATION_INTERNAL_BRANCH: default is master
    * ROUTE53_ZONE: AWS route53 zone for getting DNS records (requried)
    * AWS_REGION: region of running sandbox instances, default is us-east-1
    * NOTIFY_ON_FAILURE: alert@example.com
    * FOLDER_NAME: folder

 This job expects the following credentials to be defined on the folder
    tools-edx-jenkins-aws-credentials: file with key/secret in boto config format
    launch-sandboxes-role-arn: the role to aws sts assume-role
 
*/

package devops.jobs
import static org.edx.jenkins.dsl.Constants.common_logrotator
import static org.edx.jenkins.dsl.Constants.common_wrappers

class SandboxTermination{
    public static def job = { dslFactory, extraVars ->
        dslFactory.job(extraVars.get("FOLDER_NAME","Monitoring") + "/sandbox-termination") {
            
            logRotator common_logrotator
            wrappers common_wrappers

            wrappers{
                credentialsBinding{
                    string('ROLE_ARN', "launch-sandboxes-role-arn")
                    string('EDX_GIT_BOT_TOKEN', "edx_git_bot_token")
                    string("GENIE_KEY", "opsgenie_heartbeat_key")
                }
            }

            assert extraVars.containsKey('JENKINS_DSL_INTERNAL_REPO') : "Please define a repo where the sandbox termination script is located"

            def gitCredentialId = extraVars.get('SECURE_GIT_CREDENTIALS','')

            parameters{
                stringParam('CONFIGURATION_REPO', extraVars.get('CONFIGURATION_REPO', 'https://github.com/edx/configuration.git'),
                            'Git repo containing edX configuration.')
                stringParam('CONFIGURATION_BRANCH', extraVars.get('CONFIGURATION_BRANCH', 'master'),
                        'e.g. tagname or origin/branchname')
                stringParam('JENKINS_DSL_INTERNAL_REPO', extraVars.get('JENKINS_DSL_INTERNAL_REPO'),
                        'Git repo containing the sandbox termination script.')
                stringParam('JENKINS_DSL_INTERNAL_BRANCH', extraVars.get('JENKINS_DSL_INTERNAL_BRANCH', 'master'),
                        'e.g. tagname or origin/branchname')
            }

            multiscm{
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
                git {
                    remote {
                        url('$JENKINS_DSL_INTERNAL_REPO')
                        branch('$JENKINS_DSL_INTERNAL_BRANCH')
                        if (gitCredentialId) {
                            credentials(gitCredentialId)
                        }
                    }
                    extensions {
                        cleanAfterCheckout()
                        pruneBranches()
                        relativeTargetDirectory('jenkins-job-dsl-internal')
                    }
                }
            }
            
            triggers{
                cron("H 14 * * *")
            }

            assert extraVars.containsKey('ROUTE53_ZONE') : "Please define a route53 zone"

            environmentVariables{
                env("ROUTE53_ZONE", extraVars.get("ROUTE53_ZONE"))
                env("NOOP", extraVars.get("NOOP", false))
                env("AWS_REGION", extraVars.get("AWS_REGION", "us-east-1"))
            }

            steps {
               shell(dslFactory.readFileFromWorkspace('devops/resources/sandbox-termination.sh'))

                String opsgenie_heartbeat_name = extraVars.get('OPSGENIE_HEARTBEAT_NAME','')
                if (opsgenie_heartbeat_name) {
                    shell('curl -X GET "https://api.opsgenie.com/v2/heartbeats/'+opsgenie_heartbeat_name+'/ping" -H "Authorization: GenieKey ${GENIE_KEY}"')
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

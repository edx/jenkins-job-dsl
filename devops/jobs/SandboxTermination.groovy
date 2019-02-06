/*
 Variables without defaults are marked (required) 
 
 Variables consumed for this job:
    * NOOP: boolean value to perform no operations, default is false
    * SECURE_GIT_CREDENTIALS: secure-bot-user (required)
    * SYSADMIN_REPO: repository containing sandbox termination python script (required)
    * SYSADMIN_BRANCH: default is master
    * CONFIGURATION_REPO: name of config repo, default is https://github.com/edx/configuration.git
    * CONFIGURATION_BRANCH: default is master
    * CONFIGURAITON_INTERNAL_REPO: Git repo containing internal overrides, default is git@github.com:edx/edx-internal.git
    * CONFIGURATION_INTERNAL_BRANCH: default is master
    * ROUTE53_ZONE: AWS route53 zone for getting DNS records (requried)
    * AWS_REGION: region of running sandbox instances, default is us-east-1
    * NOTIFY_ON_FAILURE: alert@example.com
    * FOLDER_NAME: folder
    * HIPCHAT_ROOM: name of the room to send hipchat notifications (required)

 This job expects the following credentials to be defined on the folder
    tools-edx-jenkins-aws-credentials: file with key/secret in boto config format
    launch-sandboxes-role-arn: the role to aws sts assume-role
    hipchat-api-key: hipchat token
 
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
                    file('AWS_CONFIG_FILE','tools-edx-jenkins-aws-credentials')
                    string('ROLE_ARN', "launch-sandboxes-role-arn")
                    string('HIPCHAT_API_KEY', "hipchat-api-key")
                    string('EDX_GIT_BOT_TOKEN', "edx_git_bot_token")
                }
            }

            assert extraVars.containsKey('SYSADMIN_REPO') : "Please define a system admin repo where the sandbox termination script is located"

            def gitCredentialId = extraVars.get('SECURE_GIT_CREDENTIALS','')

            parameters{
                stringParam('CONFIGURATION_REPO', extraVars.get('CONFIGURATION_REPO', 'https://github.com/edx/configuration.git'),
                            'Git repo containing edX configuration.')
                stringParam('CONFIGURATION_BRANCH', extraVars.get('CONFIGURATION_BRANCH', 'master'),
                        'e.g. tagname or origin/branchname')
                stringParam('SYSADMIN_REPO', extraVars.get('SYSADMIN_REPO'),
                        'Git repo containing sysadmin configuration which contains the sandbox termination script.')
                stringParam('SYSADMIN_BRANCH', extraVars.get('SYSADMIN_BRANCH', 'master'),
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
                        url('$SYSADMIN_REPO')
                        branch('$SYSADMIN_BRANCH')
                        if (gitCredentialId) {
                            credentials(gitCredentialId)
                        }
                    }
                    extensions {
                        cleanAfterCheckout()
                        pruneBranches()
                        relativeTargetDirectory('sysadmin')
                    }
                }
            }
            
            triggers{
                cron("H 14 * * *")
            }

            assert extraVars.containsKey('ROUTE53_ZONE') : "Please define a route53 zone"
            assert extraVars.containsKey('HIPCHAT_ROOM') : "Please define a hipchat room to send notifications to"

            environmentVariables{
                env("ROUTE53_ZONE", extraVars.get("ROUTE53_ZONE"))
                env("NOOP", extraVars.get("NOOP", false))
                env("AWS_REGION", extraVars.get("AWS_REGION", "us-east-1"))
                env("HIPCHAT_ROOM", extraVars.get("HIPCHAT_ROOM"))
                env("SNITCH", extraVars.get("SNITCH"))
            }

            steps {
                virtualenv {
                    nature("shell")
                    systemSitePackages(false)

                    command(
                        dslFactory.readFileFromWorkspace("devops/resources/sandbox-termination.sh")
                    )

                }
            }

            if (extraVars.get('NOTIFY_ON_FAILURE')){
                publishers {
                    mailer(extraVars.get('NOTIFY_ON_FAILURE'), false, false)
                }
            } 

        }
    }

}

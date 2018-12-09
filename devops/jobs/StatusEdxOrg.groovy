/*
 update the status.edx.org website

 Variables without defaults are marked (required) 
 
 Variables consumed for this job:
    * SECURE_GIT_CREDENTIALS: secure-bot-user (required)
    * NOTIFY_ON_FAILURE: alert@example.com
    * FOLER_NAME: folder
    * STATUS_REPO: default is git@github.com:edX/status.edx.org
    * STATUS_BRANCH: default is master
*/

package devops.jobs
import static org.edx.jenkins.dsl.Constants.common_logrotator
import static org.edx.jenkins.dsl.Constants.common_wrappers


class StatusEdxOrg {

    public static def job = { dslFactory, extraVars ->
        dslFactory.job(extraVars.get("FOLDER_NAME","Monitoring") + "/status-edx-org") {
               
            logRotator common_logrotator
            wrappers common_wrappers

            wrappers{
                credentialsBinding{
                    file('AWS_CONFIG_FILE','tools-edx-jenkins-aws-credentials')
                    string('ROLE_ARN', 'tools-jenkins-to-status-edx-org-role-arn')
                }
            }

            def gitCredentialId = extraVars.get('SECURE_GIT_CREDENTIALS','')

            parameters{
                stringParam('CONFIGURATION_REPO', extraVars.get('CONFIGURATION_REPO', 'https://github.com/edx/configuration.git'),
                            'Git repo containing edX configuration.')
                stringParam('CONFIGURATION_BRANCH', extraVars.get('CONFIGURATION_BRANCH', 'master'),
                        'e.g. tagname or origin/branchname')
                stringParam('STATUS_REPO', extraVars.get('STATUS_REPO', 'git@github.com:edx/status.edx.org'),
                        'Git repo containing status.edx.org configuration.')
                stringParam('STATUS_BRANCH', extraVars.get('STATUS_BRANCH', 'master'),
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
                        url('$STATUS_REPO')
                        branch('$STATUS_BRANCH')
                        if (gitCredentialId) {
                            credentials(gitCredentialId)
                        }
                    }
                    extensions {
                        pruneBranches()
                        relativeTargetDirectory('status.edx.org')
                    }
                }
            }

            triggers{
                cron('H/10 * * * *')
            }

            steps {
                virtualenv {
                    nature("shell")
                    systemSitePackages(false)

                    command(
                        dslFactory.readFileFromWorkspace("devops/resources/status-edx-org.sh")
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

/*
 Create Data Czar

 Variables without defaults are marked (required) 
 
 Variables consumed for this job:
    * SECURE_GIT_CREDENTIALS: secure-bot-user (required)
    * NOTIFY_ON_FAILURE: alert@example.com
    * FOLER_NAME: folder
    * SYS_ADMIN_REPO: repo where the mongo backup script is located (required)
    * STATUS_BRANCH: default is master
    * ORGANIZATION: organization name to create data czar for
    * GPG_KEY: Key required for encryptring aws credentials
    This job expects the following credentials to be defined on the folder
    tools-edx-jenkins-aws-credentials: file with key/secret in boto config format
    

*/
package devops.jobs
import static org.edx.jenkins.dsl.Constants.common_logrotator
import static org.edx.jenkins.dsl.Constants.common_wrappers

class CreateDataCzar{
    public static def job = { dslFactory, extraVars ->
        dslFactory.job(extraVars.get("FOLDER_NAME","Data Czar") + "/create-data-czar") {

            logRotator common_logrotator
            wrappers common_wrappers

            wrappers {
                sshAgent(extraVars.get("SSH_AGENT_KEY"))
            }

            parameters{
                stringParam('SYS_ADMIN_REPO', extraVars.get('SYS_ADMIN_REPO', 'git@github.com:edx-ops/sysadmin.git'),
                        'Git repo containing scripts for data czar creation.')
                stringParam('SYS_ADMIN_REPO_BRANCH', extraVars.get('SYS_ADMIN_REPO_BRANCH', 'master'),
                        'e.g. tagname or origin/branchname')
                stringParam('ORGANIZATION', extraVars.get('ORGANIZATION'),
                        'Name of organization to create data czar. e.g alaskax')
                textParam('GPG_KEY', extraVars.get('GPG_KEY'),
                        'Paste the GPG key for encryption of aws credentials')
            }

            multiscm{
                git {
                    remote {
                        url('$SYS_ADMIN_REPO')
                        branch('$SYS_ADMIN_REPO_BRANCH')
                    }
                    extensions {
                        cleanAfterCheckout()
                        pruneBranches()
                        relativeTargetDirectory('sysadmin')
                    }
                }
            }

            // triggers{
            //     cron("H H * * *")
            // }

            // environmentVariables{
            //     env("s3_bucket", extraVars.get("S3_BUCKET"))
            //     env("host_ip", extraVars.get("HOST_IP"))
            //     env("splunk_host", extraVars.get("SPLUNK_HOST"))
            //     env("splunk_backup_dir", extraVars.get("SPLUNK_BACKUP_DIR"))
            // }

            steps {
               shell(dslFactory.readFileFromWorkspace('devops/resources/backup-splunk-to-s3.sh'))

            }

            if (extraVars.get('NOTIFY_ON_FAILURE')){
                publishers {
                    mailer(extraVars.get('NOTIFY_ON_FAILURE'), false, false)
                }
            }

        }
    }

}

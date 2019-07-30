package devops.jobs
import static org.edx.jenkins.dsl.Constants.common_logrotator
import static org.edx.jenkins.dsl.Constants.common_wrappers

class BackupSplunkToS3{
    public static def job = { dslFactory, extraVars ->
        dslFactory.job(extraVars.get("FOLDER_NAME","Monitoring") + "/backup-tools-edx-splunk-config-to-s3") {

            logRotator common_logrotator
            wrappers common_wrappers

            wrappers {
                sshAgent(extraVars.get("SSH_AGENT_KEY"))
            }

            parameters{
                stringParam('CONFIGURATION_REPO', extraVars.get('CONFIGURATION_REPO', 'git@github.com:edx/configuration.git'),
                        'Git repo containing edX configuration.')
                stringParam('CONFIGURATION_BRANCH', extraVars.get('CONFIGURATION_BRANCH', 'master'),
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
            }

            triggers{
                cron("H H * * *")
            }

            environmentVariables{
                env("s3_bucket", extraVars.get("S3_BUCKET"))
                env("host_ip", extraVars.get("HOST_IP"))
                env("splunk_host", extraVars.get("SPLUNK_HOST"))
                env("splunk_backup_dir", extraVars.get("SPLUNK_BACKUP_DIR:"))
            }

            steps {
                virtualenv {
                    nature("shell")
                    systemSitePackages(false)

                    command(
                            dslFactory.readFileFromWorkspace("devops/resources/backup-splunk-to-s3.sh")
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

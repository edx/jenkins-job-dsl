package analytics
import static org.edx.jenkins.dsl.AnalyticsConstants.common_log_rotator
import static org.edx.jenkins.dsl.AnalyticsConstants.common_wrappers
import static org.edx.jenkins.dsl.AnalyticsConstants.common_publishers
import static org.edx.jenkins.dsl.AnalyticsConstants.common_triggers

class BackupVertica {
    public static def job = { dslFactory, allVars ->
        allVars.get('VERTICA_BACKUPS').each { backup, backup_config ->
            dslFactory.job("backup-vertica-$backup") {
                logRotator common_log_rotator(backup_config)
                publishers common_publishers(backup_config)
                triggers common_triggers(backup_config)
                parameters {
                    textParam('BACKUP_CMD', backup_config.get('VERTICA_BACKUP_CMD'), '')
                    stringParam('NOTIFY', '$PAGER_NOTIFY', 'Space separated list of emails to send notifications to.')
                }
                environmentVariables {
                    env('OPSGENIE_HEARTBEAT_NAME', env_config.get('OPSGENIE_HEARTBEAT_NAME'))
                    env('OPSGENIE_HEARTBEAT_DURATION_NUM', env_config.get('OPSGENIE_HEARTBEAT_DURATION_NUM'))
                    env('OPSGENIE_HEARTBEAT_DURATION_UNIT', env_config.get('OPSGENIE_HEARTBEAT_DURATION_UNIT'))
                }
                wrappers common_wrappers(backup_config)
                wrappers {
                    credentialsBinding {
                        string('OPSGENIE_HEARTBEAT_CONFIG_KEY', 'opsgenie_heartbeat_config_key')
                    }
                }
                steps {
                    shell(dslFactory.readFileFromWorkspace('dataeng/resources/opsgenie-enable-heartbeat.sh'))
                    shell(dslFactory.readFileFromWorkspace('dataeng/resources/backup-vertica.sh'))
                    shell(dslFactory.readFileFromWorkspace('dataeng/resources/opsgenie-disable-heartbeat.sh'))
                }
            }
        }
    }
}

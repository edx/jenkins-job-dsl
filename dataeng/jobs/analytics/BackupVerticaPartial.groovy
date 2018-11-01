package analytics
import static org.edx.jenkins.dsl.AnalyticsConstants.common_log_rotator
import static org.edx.jenkins.dsl.AnalyticsConstants.common_wrappers
import static org.edx.jenkins.dsl.AnalyticsConstants.common_publishers
import static org.edx.jenkins.dsl.AnalyticsConstants.common_triggers

class BackupVerticaPartial {
    public static def backup_vertica_partial_job = { dslFactory, allVars ->
        dslFactory.job("backup-vertica-partial") {
            logRotator common_log_rotator(allVars)
            wrappers common_wrappers(allVars)
            publishers common_publishers(allVars)
            triggers common_triggers(allVars)
            parameters {
                textParam('BACKUP_SNIPPET', allVars.get('VERTICA_BACKUP_SNIPPET'), '')
                stringParam('NOTIFY', '$PAGER_NOTIFY', 'Space separated list of emails to send notifications to.')
            }
            steps {
                shell(dslFactory.readFileFromWorkspace('dataeng/resources/backup-vertica-partial.sh'))
                if (allVars.get('SNITCH')) {
                    shell('curl https://nosnch.in/' + allVars.get('SNITCH'))
                }
            }
        }
    }

}

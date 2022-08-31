package analytics
import static org.edx.jenkins.dsl.AnalyticsConstants.secure_scm
import static org.edx.jenkins.dsl.AnalyticsConstants.common_log_rotator
import static org.edx.jenkins.dsl.AnalyticsConstants.common_wrappers
import static org.edx.jenkins.dsl.AnalyticsConstants.common_publishers
import static org.edx.jenkins.dsl.AnalyticsConstants.common_triggers
import static org.edx.jenkins.dsl.AnalyticsConstants.secure_scm_parameters

class TableauRestore{
    public static def job = { dslFactory, allVars ->
        dslFactory.job("tableau-restore"){
            description('This job restores tableau data and config backup to a Tableau Server.')
            logRotator common_log_rotator(allVars)
            parameters secure_scm_parameters(allVars)
            parameters {
                stringParam('TABLEAU_SERVER_HOST', '', 'Address of Tableau Server.')
                stringParam('USER_NAME', '', 'SSH User name.')
                stringParam('TABLEAU_ADMIN_USER', '', 'User which can invoke tsm commands.')
                stringParam('S3_PATH', allVars.get('S3_PATH'), 'S3 path containing the backup files.')
                stringParam('BACKUP_TIMESTAMP', '', 'Timestamp of the backup to restore.')
                stringParam('NOTIFY', allVars.get('NOTIFY','$PAGER_NOTIFY'), 'Space separated list of emails to send notifications to.')
            }
            wrappers {
                colorizeOutput('xterm')
            }
            wrappers common_wrappers(allVars)
            publishers common_publishers(allVars)
            steps {
                shell(dslFactory.readFileFromWorkspace('dataeng/resources/tableau-restore.sh'))
            }
        }
    }
}

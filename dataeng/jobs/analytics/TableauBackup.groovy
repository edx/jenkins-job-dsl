package analytics
import static org.edx.jenkins.dsl.AnalyticsConstants.secure_scm
import static org.edx.jenkins.dsl.AnalyticsConstants.common_log_rotator
import static org.edx.jenkins.dsl.AnalyticsConstants.common_wrappers
import static org.edx.jenkins.dsl.AnalyticsConstants.common_publishers
import static org.edx.jenkins.dsl.AnalyticsConstants.common_triggers
import static org.edx.jenkins.dsl.AnalyticsConstants.secure_scm_parameters

class TableauBackup{
    public static def job = { dslFactory, allVars ->
        dslFactory.job("tableau-backup-weekly"){
            logRotator common_log_rotator(allVars)
            parameters secure_scm_parameters(allVars)
            parameters {
                stringParam('TABLEAU_SERVER_IP', allVars.get('TABLEAU_SERVER_IP'), 'IP address of Tableau Server.')
                stringParam('USER_NAME', allVars.get('USER_NAME'), 'SSH User name.')
                stringParam('S3_BUCKET', allVars.get('S3_BUCKET'), 'S3 backup bucket name with path.')
                stringParam('NOTIFY', allVars.get('NOTIFY','$PAGER_NOTIFY'), 'Space separated list of emails to send notifications to.')
            }
            triggers common_triggers(allVars)
            wrappers {
                colorizeOutput('xterm')
            }
            wrappers common_wrappers(allVars)
            publishers common_publishers(allVars)
            steps {
                shell(dslFactory.readFileFromWorkspace('dataeng/resources/tableau-backup.sh'))
            }
        }
    }
}

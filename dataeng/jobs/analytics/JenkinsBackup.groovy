package analytics
import static org.edx.jenkins.dsl.AnalyticsConstants.common_wrappers
import static org.edx.jenkins.dsl.AnalyticsConstants.common_triggers
import static org.edx.jenkins.dsl.AnalyticsConstants.common_publishers

class JenkinsBackup {
    public static def job = { dslFactory, allVars ->
        dslFactory.job('jenkins-backup') {
            parameters {
                stringParam('S3_BACKUP_BUCKET', allVars.get('S3_BACKUP_BUCKET'))
                stringParam('NOTIFY', allVars.get('NOTIFY','$PAGER_NOTIFY'), 'Space separated list of emails to send notifications to.')
                stringParam('PYTHON_VENV_VERSION', 'python3.8', 'Python virtual environment version to used.')
            }
            wrappers common_wrappers(allVars)
            triggers common_triggers(allVars)
            publishers common_publishers(allVars)
            steps {
                shell(dslFactory.readFileFromWorkspace('dataeng/resources/jenkins-backup.sh'))
            }
        }
    }
}

package analytics
import static org.edx.jenkins.dsl.AnalyticsConstants.common_wrappers
import static org.edx.jenkins.dsl.AnalyticsConstants.common_triggers
import static org.edx.jenkins.dsl.AnalyticsConstants.common_publishers
import static org.edx.jenkins.dsl.AnalyticsConstants.common_groovy_postbuild
import static org.edx.jenkins.dsl.AnalyticsConstants.common_datadog_build_end

class JenkinsBackup {
    public static def job = { dslFactory, allVars ->
        dslFactory.job('jenkins-backup') {
            parameters {
                stringParam('S3_BACKUP_BUCKET', allVars.get('S3_BACKUP_BUCKET'))
                stringParam('NOTIFY', allVars.get('NOTIFY','$PAGER_NOTIFY'), 'Space separated list of emails to send notifications to.')
                stringParam('PYTHON_VENV_VERSION', 'python3.8', 'Python virtual environment version to used.')
                stringParam('BUILD_STATUS')
            }
            wrappers common_wrappers(allVars)
            triggers common_triggers(allVars)
            publishers common_datadog_build_end(dslFactory, allVars) << common_groovy_postbuild(dslFactory, allVars) << common_publishers(allVars)
            steps {
                shell(dslFactory.readFileFromWorkspace('dataeng/resources/datadog_job_start.sh'))
                shell(dslFactory.readFileFromWorkspace('dataeng/resources/jenkins-backup.sh'))
            }
        }
    }
}

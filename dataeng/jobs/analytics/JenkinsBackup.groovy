package analytics
import static org.edx.jenkins.dsl.AnalyticsConstants.common_wrappers
import static org.edx.jenkins.dsl.AnalyticsConstants.common_triggers
import static org.edx.jenkins.dsl.AnalyticsConstants.common_publishers

class JenkinsBackup {
    public static def job = { dslFactory, allVars ->
        dslFactory.job('jenkins-backup') {
            parameters {
                stringParam('S3_BACKUP_BUCKET', allVars.get('S3_BACKUP_BUCKET'))
            }
            wrappers common_wrappers(allVars)
            triggers common_triggers(allVars)
            publishers common_publishers(allVars)
            steps {
                virtualenv {
                    nature("shell")
                    command(
                        dslFactory.readFileFromWorkspace("dataeng/resources/jenkins-backup.sh")
                    )
                }
            }
        }
    }
}

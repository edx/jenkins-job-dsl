package analytics
import static org.edx.jenkins.dsl.AnalyticsConstants.common_triggers
import static org.edx.jenkins.dsl.AnalyticsConstants.common_log_rotator
import static org.edx.jenkins.dsl.AnalyticsConstants.common_publishers

class VerticaDiskUsageMonitor {
    public static def job = { dslFactory, allVars ->
        dslFactory.job('vertica-disk-usage-monitor') {
            parameters {
                stringParam('TOOLS_REPO', allVars.get('TOOLS_REPO_URL'), '')
                stringParam('TOOLS_BRANCH', 'origin/master', '')
                stringParam('THRESHOLD', allVars.get('THRESHOLD'), 'Utilization threshold for alarm.')
                stringParam('CONFIG_FILE_PATH', allVars.get('CONFIG_FILE_PATH'))
                stringParam('NOTIFY', '$PAGER_NOTIFY', 'Space separated list of emails to send notifications to.')
            }
            logRotator common_log_rotator(allVars)
            multiscm {
                git {
                    remote {
                        url('$TOOLS_REPO')
                        branch('$TOOLS_BRANCH')
                        credentials('1')
                    }
                }
            }
            triggers common_triggers(allVars)
            wrappers {
                timestamps()
            }
            publishers {
                postBuildTask {
                    task('Error we are approaching our license usage capacity', 'exit 1', true, true)
                }
            }
            publishers common_publishers(allVars)
            steps {
                virtualenv {
                    nature("shell")
                    command(
                        dslFactory.readFileFromWorkspace("dataeng/resources/vertica-disk-usage-monitor.sh")
                    )
                }
            }
        }
    }
}

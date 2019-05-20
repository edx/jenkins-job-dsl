package analytics
import static org.edx.jenkins.dsl.AnalyticsConstants.common_triggers
import static org.edx.jenkins.dsl.AnalyticsConstants.common_log_rotator
import static org.edx.jenkins.dsl.AnalyticsConstants.common_publishers

class EmrCostReporter {
    public static def job = { dslFactory, allVars ->
        dslFactory.job('emr-cost-reporter') {
            parameters {
                stringParam('TOOLS_REPO', allVars.get('TOOLS_REPO_URL'), '')
                stringParam('TOOLS_BRANCH', 'origin/master', '')
                stringParam('THRESHOLD', allVars.get('THRESHOLD'), 'Number of dollars "budgeted".  Going over this threshold will cause the job to fail.')
                stringParam('WEEKLY_JOB_THRESHOLD_ADJUSTMENT', allVars.get('WEEKLY_JOB_THRESHOLD_ADJUSTMENT'), '')
                stringParam('GRAPHITE_HOST', allVars.get('GRAPHITE_HOST'), '')
                stringParam('NOTIFY', allVars.get('NOTIFY','$PAGER_NOTIFY'), 'Space separated list of emails to send notifications to.')
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
            publishers common_publishers(allVars)
            steps {
                virtualenv {
                    nature("shell")
                    command(
                        dslFactory.readFileFromWorkspace("dataeng/resources/emr-cost-reporter.sh")
                    )
                }
            }
        }
    }
}

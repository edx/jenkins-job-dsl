package analytics
import static org.edx.jenkins.dsl.AnalyticsConstants.common_log_rotator
import static org.edx.jenkins.dsl.AnalyticsConstants.common_wrappers
import static org.edx.jenkins.dsl.AnalyticsConstants.common_triggers

class MonitorBigqueryEventLoading {
    public static def job = { dslFactory, allVars ->
        dslFactory.job("monitor-bigquery-loading") {

            // BigQuery deprecation
            disabled(true)
            
            logRotator common_log_rotator(allVars)
            parameters {
                stringParam('TOOLS_REPO', allVars.get('TOOLS_REPO_URL'), '')
                stringParam('TOOLS_BRANCH', allVars.get('TOOLS_BRANCH'), 'e.g. tagname or origin/branchname')
                stringParam('PARTITION_DATE', allVars.get('PARTITION_DATE'), 'DATE to check bigquery event loading.')
                stringParam('CREDENTIALS', allVars.get('CREDENTIALS'))
            }
            multiscm {
                git {
                    remote {
                        url('$TOOLS_REPO')
                        branch('$TOOLS_BRANCH')
                    }
                    extensions {
                        relativeTargetDirectory('analytics-tools')
                        pruneBranches()
                        cleanAfterCheckout()
                    }
                }
            }
            triggers common_triggers(allVars)
            wrappers common_wrappers(allVars)
            steps {
                shell(dslFactory.readFileFromWorkspace('dataeng/resources/monitor-bigquery-loading.sh'))
            }
        }
    }
}

package analytics
import static org.edx.jenkins.dsl.AnalyticsConstants.common_multiscm
import static org.edx.jenkins.dsl.AnalyticsConstants.common_parameters
import static org.edx.jenkins.dsl.AnalyticsConstants.common_log_rotator
import static org.edx.jenkins.dsl.AnalyticsConstants.common_wrappers
import static org.edx.jenkins.dsl.AnalyticsConstants.common_publishers
import static org.edx.jenkins.dsl.AnalyticsConstants.common_triggers

class TotalEventsDailyReport {
    public static def job = { dslFactory, allVars ->
        dslFactory.job("total-events-daily-report") {
            logRotator common_log_rotator(allVars)
            parameters common_parameters(allVars)
            parameters {
                stringParam('DAY_TO_REPORT', allVars.get('DAY_TO_REPORT'), '')
                stringParam('CREDENTIALS', allVars.get('CREDENTIALS'), '')
                stringParam('S3_DIR', allVars.get('S3_DIR'), '')
            }
            multiscm common_multiscm(allVars)
            triggers common_triggers(allVars)
            wrappers common_wrappers(allVars)
            publishers common_publishers(allVars)
            steps {
                shell(dslFactory.readFileFromWorkspace('dataeng/resources/total-events-daily-report.sh'))
            }
        }
    }
}

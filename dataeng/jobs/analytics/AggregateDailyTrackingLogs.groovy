package analytics
import static org.edx.jenkins.dsl.AnalyticsConstants.to_date_interval_parameter
import static org.edx.jenkins.dsl.AnalyticsConstants.common_parameters
import static org.edx.jenkins.dsl.AnalyticsConstants.common_log_rotator
import static org.edx.jenkins.dsl.AnalyticsConstants.common_wrappers
import static org.edx.jenkins.dsl.AnalyticsConstants.common_publishers
import static org.edx.jenkins.dsl.AnalyticsConstants.common_triggers

import static org.edx.jenkins.dsl.AnalyticsConstants.common_multiscm

class AggregateDailyTrackingLogs {
    public static def job = { dslFactory, allVars ->
        dslFactory.job("aggregate-daily-tracking-logs") {
            logRotator common_log_rotator(allVars)
            parameters to_date_interval_parameter(allVars)
            parameters common_parameters(allVars)
            parameters {
                stringParam('SOURCE_BUCKET_PATH', allVars.get('SOURCE_BUCKET_PATH'))
                stringParam('DEST_BUCKET_PATH', allVars.get('DEST_BUCKET_PATH'))
                stringParam('TARGET_SIZE', allVars.get('TARGET_SIZE'))
            }
            multiscm common_multiscm(allVars)
            triggers common_triggers(allVars)
            wrappers common_wrappers(allVars)
            publishers common_publishers(allVars)
            steps {
                shell(dslFactory.readFileFromWorkspace('dataeng/resources/aggregate-daily-tracking-logs.sh'))
            }
        }
    }
}


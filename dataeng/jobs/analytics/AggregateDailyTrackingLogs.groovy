package analytics
import static org.edx.jenkins.dsl.AnalyticsConstants.to_date_interval_parameter
import static org.edx.jenkins.dsl.AnalyticsConstants.common_parameters
import static org.edx.jenkins.dsl.AnalyticsConstants.common_log_rotator
import static org.edx.jenkins.dsl.AnalyticsConstants.common_wrappers
import static org.edx.jenkins.dsl.AnalyticsConstants.common_publishers
import static org.edx.jenkins.dsl.AnalyticsConstants.common_triggers
import static org.edx.jenkins.dsl.AnalyticsConstants.common_groovy_postbuild
import static org.edx.jenkins.dsl.AnalyticsConstants.common_datadog_build_end
import static org.edx.jenkins.dsl.AnalyticsConstants.common_multiscm

class AggregateDailyTrackingLogs {
    public static def job = { dslFactory, allVars ->
        allVars.get('ENVIRONMENTS').each { environment, env_config ->
            dslFactory.job("aggregate-daily-tracking-logs-$environment") {
                disabled(env_config.get('DISABLED', false))
                logRotator common_log_rotator(allVars, env_config)
                parameters to_date_interval_parameter(env_config)
                parameters common_parameters(allVars, env_config)
                parameters {
                    stringParam('SOURCE_BUCKET_PATH', env_config.get('SOURCE_BUCKET_PATH'))
                    stringParam('DEST_BUCKET_PATH', env_config.get('DEST_BUCKET_PATH'))
                    stringParam('TARGET_SIZE', env_config.get('TARGET_SIZE'))
                }
                multiscm common_multiscm(allVars)
                triggers common_triggers(allVars, env_config)
                wrappers common_wrappers(allVars)
                publishers common_datadog_build_end(dslFactory, allVars) << common_groovy_postbuild(dslFactory, allVars) << common_publishers(allVars)
                steps {
                    shell(dslFactory.readFileFromWorkspace('dataeng/resources/datadog_job_start.sh'))
                    shell(dslFactory.readFileFromWorkspace('dataeng/resources/aggregate-daily-tracking-logs.sh'))
                }
            }
        }
    }
}

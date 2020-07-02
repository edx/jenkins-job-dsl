package analytics
import static org.edx.jenkins.dsl.AnalyticsConstants.common_multiscm
import static org.edx.jenkins.dsl.AnalyticsConstants.common_parameters
import static org.edx.jenkins.dsl.AnalyticsConstants.from_date_interval_parameter
import static org.edx.jenkins.dsl.AnalyticsConstants.to_date_interval_parameter
import static org.edx.jenkins.dsl.AnalyticsConstants.common_log_rotator
import static org.edx.jenkins.dsl.AnalyticsConstants.common_wrappers
import static org.edx.jenkins.dsl.AnalyticsConstants.common_publishers
import static org.edx.jenkins.dsl.AnalyticsConstants.common_triggers

class EventTypeDistribution {
    public static def job = { dslFactory, allVars ->
        dslFactory.job("event-type-distribution") {

            // DENG-317
            disabled(true)

            logRotator common_log_rotator(allVars)
            parameters {
                stringParam('SOURCE', allVars.get('PRODUCTION_EVENT_LOGS_SOURCE'), '')
                stringParam('OUTPUT_URL', allVars.get('OUTPUT_URL'), '')
                stringParam('EVENTS_LIST', allVars.get('EVENTS_LIST'), '')
                stringParam('CREDENTIALS', allVars.get('CREDENTIALS'), '')
            }
            parameters common_parameters(allVars)
            parameters from_date_interval_parameter(allVars)
            parameters to_date_interval_parameter(allVars)
            multiscm common_multiscm(allVars)
            triggers common_triggers(allVars)
            wrappers common_wrappers(allVars)
            publishers common_publishers(allVars)
            steps {
                shell(dslFactory.readFileFromWorkspace('dataeng/resources/event-type-distribution.sh'))
            }
        }
    }
}

package analytics
import static org.edx.jenkins.dsl.AnalyticsConstants.common_multiscm
import static org.edx.jenkins.dsl.AnalyticsConstants.common_parameters
import static org.edx.jenkins.dsl.AnalyticsConstants.from_date_interval_parameter
import static org.edx.jenkins.dsl.AnalyticsConstants.to_date_interval_parameter
import static org.edx.jenkins.dsl.AnalyticsConstants.secure_scm_parameters
import static org.edx.jenkins.dsl.AnalyticsConstants.common_log_rotator
import static org.edx.jenkins.dsl.AnalyticsConstants.common_wrappers
import static org.edx.jenkins.dsl.AnalyticsConstants.common_publishers
import static org.edx.jenkins.dsl.AnalyticsConstants.common_triggers

class LoadEventsToVertica {
    public static def job = { dslFactory, allVars ->
        dslFactory.job("load-events-to-vertica") {
            logRotator common_log_rotator(allVars)
            parameters common_parameters(allVars)
            parameters from_date_interval_parameter(allVars)
            parameters to_date_interval_parameter(allVars)
            parameters secure_scm_parameters(allVars)
            parameters {
                stringParam('OUTPUT_URL', allVars.get('OUTPUT_URL'))
                stringParam('CREDENTIALS', allVars.get('CREDENTIALS'))
                stringParam('EVENTS_LIST', allVars.get('EVENTS_LIST'))
                stringParam('SCHEMA', allVars.get('SCHEMA'))
            }
            multiscm common_multiscm(allVars)
            wrappers common_wrappers(allVars)
            publishers common_publishers(allVars)
            steps {
                shell(dslFactory.readFileFromWorkspace('dataeng/resources/load-events-to-vertica.sh'))
            }
        }
    }
}

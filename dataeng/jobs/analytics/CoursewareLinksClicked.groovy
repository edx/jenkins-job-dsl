package analytics
import static org.edx.jenkins.dsl.AnalyticsConstants.common_multiscm
import static org.edx.jenkins.dsl.AnalyticsConstants.common_parameters
import static org.edx.jenkins.dsl.AnalyticsConstants.from_date_interval_parameter
import static org.edx.jenkins.dsl.AnalyticsConstants.to_date_interval_parameter
import static org.edx.jenkins.dsl.AnalyticsConstants.common_log_rotator
import static org.edx.jenkins.dsl.AnalyticsConstants.common_wrappers
import static org.edx.jenkins.dsl.AnalyticsConstants.common_publishers
import static org.edx.jenkins.dsl.AnalyticsConstants.common_triggers

class CoursewareLinksClicked {
    public static def job = { dslFactory, allVars ->
        dslFactory.job("courseware-links-clicked") {

            // DENG-633
            disabled(true)

            logRotator common_log_rotator(allVars)
            parameters common_parameters(allVars)
            parameters from_date_interval_parameter(allVars)
            parameters to_date_interval_parameter(allVars)
            parameters {
                stringParam('SOURCE', allVars.get('PRODUCTION_EVENT_LOGS_SOURCE'), '')
            }
            multiscm common_multiscm(allVars)
            triggers common_triggers(allVars)
            wrappers common_wrappers(allVars)
            publishers common_publishers(allVars)
            steps {
                shell(dslFactory.readFileFromWorkspace('dataeng/resources/courseware-links-clicked.sh'))
            }
        }
    }
}

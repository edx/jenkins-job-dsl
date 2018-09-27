package analytics
import static org.edx.jenkins.dsl.AnalyticsConstants.common_multiscm
import static org.edx.jenkins.dsl.AnalyticsConstants.common_parameters
import static org.edx.jenkins.dsl.AnalyticsConstants.date_interval_parameters
import static org.edx.jenkins.dsl.AnalyticsConstants.common_log_rotator
import static org.edx.jenkins.dsl.AnalyticsConstants.common_wrappers
import static org.edx.jenkins.dsl.AnalyticsConstants.common_publishers

class UserActivity {
    public static def job = { dslFactory, extraVars ->
        dslFactory.job('user-activity') {
            logRotator common_log_rotator(extraVars)
            parameters common_parameters(extraVars)
            parameters date_interval_parameters(extraVars)
            multiscm common_multiscm(extraVars)
            triggers {
                cron(extraVars.get('JOB_FREQUENCY', ''))
            }
            wrappers common_wrappers(extraVars)
            publishers common_publishers(extraVars)
            steps {
                shell(dslFactory.readFileFromWorkspace("dataeng/resources/user-activity.sh"))
            }
        }
    }
}

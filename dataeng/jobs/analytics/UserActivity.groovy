package analytics
import static org.edx.jenkins.dsl.AnalyticsConstants.common_multiscm
import static org.edx.jenkins.dsl.AnalyticsConstants.common_parameters
import static org.edx.jenkins.dsl.AnalyticsConstants.date_interval_parameters
import static org.edx.jenkins.dsl.AnalyticsConstants.common_log_rotator
import static org.edx.jenkins.dsl.AnalyticsConstants.common_wrappers
import static org.edx.jenkins.dsl.AnalyticsConstants.common_publishers

class UserActivity {
    public static def job = { dslFactory, allVars ->
        allVars.get('ENVIRONMENTS').each { environment, env_config ->
            dslFactory.job("user-activity-$environment") {
                logRotator common_log_rotator(allVars)
                parameters common_parameters(allVars, env_config)
                parameters date_interval_parameters(allVars)
                multiscm common_multiscm(allVars)
                triggers {
                    cron(env_config.get('JOB_FREQUENCY', ''))
                }
                wrappers common_wrappers(allVars)
                publishers common_publishers(allVars)
                steps {
                    shell(dslFactory.readFileFromWorkspace("dataeng/resources/user-activity.sh"))
                }
            }
        }
    }
}

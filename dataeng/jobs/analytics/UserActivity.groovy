package analytics
import static org.edx.jenkins.dsl.AnalyticsConstants.common_multiscm
import static org.edx.jenkins.dsl.AnalyticsConstants.common_parameters
import static org.edx.jenkins.dsl.AnalyticsConstants.to_date_interval_parameter
import static org.edx.jenkins.dsl.AnalyticsConstants.common_log_rotator
import static org.edx.jenkins.dsl.AnalyticsConstants.common_wrappers
import static org.edx.jenkins.dsl.AnalyticsConstants.common_publishers
import static org.edx.jenkins.dsl.AnalyticsConstants.common_triggers
import static org.edx.jenkins.dsl.AnalyticsConstants.terminate_cluster_post_build
import static org.edx.jenkins.dsl.AnalyticsConstants.timeout_wrapper

class UserActivity {
    public static def job = { dslFactory, allVars ->
        allVars.get('ENVIRONMENTS').each { environment, env_config ->
            dslFactory.job("user-activity-$environment") {
                logRotator common_log_rotator(allVars, env_config)
                parameters common_parameters(allVars, env_config)
                parameters to_date_interval_parameter(allVars)
                multiscm common_multiscm(allVars)
                triggers common_triggers(allVars, env_config)
                wrappers common_wrappers(allVars)
                wrappers timeout_wrapper(allVars)
                publishers common_publishers(allVars)
                publishers terminate_cluster_post_build(allVars)
                steps {
                    shell(dslFactory.readFileFromWorkspace("dataeng/resources/user-activity.sh"))
                }
            }
        }
    }
}

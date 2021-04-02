package analytics
import static org.edx.jenkins.dsl.AnalyticsConstants.common_multiscm
import static org.edx.jenkins.dsl.AnalyticsConstants.common_parameters
import static org.edx.jenkins.dsl.AnalyticsConstants.to_date_interval_parameter
import static org.edx.jenkins.dsl.AnalyticsConstants.common_log_rotator
import static org.edx.jenkins.dsl.AnalyticsConstants.common_wrappers
import static org.edx.jenkins.dsl.AnalyticsConstants.common_publishers
import static org.edx.jenkins.dsl.AnalyticsConstants.common_triggers
import static org.edx.jenkins.dsl.AnalyticsConstants.opsgenie_heartbeat_publisher

class UserLocationByCourse {
    public static def job = { dslFactory, allVars ->
        allVars.get('ENVIRONMENTS').each { environment, env_config ->
            dslFactory.job("user-location-by-course-$environment") {
                logRotator common_log_rotator(allVars, env_config)
                multiscm common_multiscm(allVars)
                triggers common_triggers(allVars, env_config)
                parameters common_parameters(allVars, env_config)
                parameters to_date_interval_parameter(allVars)
                environmentVariables {
                    env('OPSGENIE_HEARTBEAT_NAME', env_config.get('OPSGENIE_HEARTBEAT_NAME'))
                    env('OPSGENIE_HEARTBEAT_DURATION_NUM', env_config.get('OPSGENIE_HEARTBEAT_DURATION_NUM'))
                    env('OPSGENIE_HEARTBEAT_DURATION_UNIT', env_config.get('OPSGENIE_HEARTBEAT_DURATION_UNIT'))
                }
                wrappers common_wrappers(allVars)
                wrappers {
                    credentialsBinding {
                        string('OPSGENIE_HEARTBEAT_CONFIG_KEY', 'opsgenie_heartbeat_config_key')
                    }
                }
                publishers common_publishers(allVars)
                publishers opsgenie_heartbeat_publisher(allVars)
                steps {
                    shell(dslFactory.readFileFromWorkspace('dataeng/resources/opsgenie-enable-heartbeat.sh'))
                    shell(dslFactory.readFileFromWorkspace('dataeng/resources/user-location-by-course.sh'))
                }
            }
        }
    }
}

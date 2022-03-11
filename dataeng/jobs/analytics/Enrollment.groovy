package analytics
import static org.edx.jenkins.dsl.AnalyticsConstants.common_authorization
import static org.edx.jenkins.dsl.AnalyticsConstants.common_multiscm
import static org.edx.jenkins.dsl.AnalyticsConstants.common_parameters
import static org.edx.jenkins.dsl.AnalyticsConstants.from_date_interval_parameter
import static org.edx.jenkins.dsl.AnalyticsConstants.to_date_interval_parameter
import static org.edx.jenkins.dsl.AnalyticsConstants.common_log_rotator
import static org.edx.jenkins.dsl.AnalyticsConstants.common_wrappers
import static org.edx.jenkins.dsl.AnalyticsConstants.common_publishers
import static org.edx.jenkins.dsl.AnalyticsConstants.common_triggers
import static org.edx.jenkins.dsl.AnalyticsConstants.opsgenie_heartbeat_publisher

class Enrollment {
    public static def job = { dslFactory, allVars ->
        allVars.get('ENVIRONMENTS').each { environment, env_config ->
            dslFactory.job("enrollment-$environment") {
                // desupport-1271: temporarily disabling now the the dbt version is live.
                // leave this for 2 weeks and delete the job when we are confident that
                // this is no longer needed.
                disabled(true)
                authorization common_authorization(env_config)
                logRotator common_log_rotator(allVars)
                multiscm common_multiscm(allVars)
                triggers common_triggers(allVars, env_config)
                publishers common_publishers(allVars)
                publishers opsgenie_heartbeat_publisher(allVars)
                parameters common_parameters(allVars, env_config)
                parameters from_date_interval_parameter(allVars)
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
                steps {
                    shell(dslFactory.readFileFromWorkspace('dataeng/resources/opsgenie-enable-heartbeat.sh'))
                    shell(dslFactory.readFileFromWorkspace('dataeng/resources/enrollment.sh'))
                }
            }
        }
    }
}

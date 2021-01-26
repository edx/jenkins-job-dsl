package analytics
import static org.edx.jenkins.dsl.AnalyticsConstants.common_multiscm
import static org.edx.jenkins.dsl.AnalyticsConstants.common_parameters
import static org.edx.jenkins.dsl.AnalyticsConstants.from_date_interval_parameter
import static org.edx.jenkins.dsl.AnalyticsConstants.to_date_interval_parameter
import static org.edx.jenkins.dsl.AnalyticsConstants.common_log_rotator
import static org.edx.jenkins.dsl.AnalyticsConstants.common_wrappers
import static org.edx.jenkins.dsl.AnalyticsConstants.common_publishers
import static org.edx.jenkins.dsl.AnalyticsConstants.common_triggers

class Enrollment {
    public static def job = { dslFactory, allVars ->
        allVars.get('ENVIRONMENTS').each { environment, env_config ->
            dslFactory.job("enrollment-$environment") {
                // As part of the MySQL upgrade for the LMS, we need to disable the jobs that
                // interface with the LMS read replica.
                // TODO: once the upgrade is complete for both prod and edge environments, remove this line
                disabled(allVars.get('JOB_DISABLED'))
                logRotator common_log_rotator(allVars)
                multiscm common_multiscm(allVars)
                triggers common_triggers(allVars, env_config)
                publishers common_publishers(allVars)
                publishers {
                    downstreamParameterized {
                        trigger("module-engagement-$environment") {
                            condition('SUCCESS')
                            parameters {
                                // The contents of this file are generated as part of the script in the build step.
                                propertiesFile('${WORKSPACE}/downstream.properties')
                            }
                        }
                    }
                }
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
                    shell(dslFactory.readFileFromWorkspace('dataeng/resources/opsgenie-disable-heartbeat.sh'))
                }
            }
        }
    }
}

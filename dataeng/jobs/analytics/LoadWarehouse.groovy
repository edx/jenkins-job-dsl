package analytics
import static org.edx.jenkins.dsl.AnalyticsConstants.common_multiscm
import static org.edx.jenkins.dsl.AnalyticsConstants.common_parameters
import static org.edx.jenkins.dsl.AnalyticsConstants.to_date_interval_parameter
import static org.edx.jenkins.dsl.AnalyticsConstants.common_log_rotator
import static org.edx.jenkins.dsl.AnalyticsConstants.common_wrappers
import static org.edx.jenkins.dsl.AnalyticsConstants.common_publishers
import static org.edx.jenkins.dsl.AnalyticsConstants.common_triggers

class LoadWarehouse {
    public static def vertica_job = { dslFactory, allVars ->
        dslFactory.job("load-warehouse") {

            // DENG-633
            disabled(true)

            logRotator common_log_rotator(allVars)
            multiscm common_multiscm(allVars)
            triggers common_triggers(allVars)
            publishers common_publishers(allVars)
            publishers {
                downstream("load-warehouse-snowflake", 'SUCCESS')
            }
            parameters common_parameters(allVars)
            parameters to_date_interval_parameter(allVars)
            parameters {
                stringParam('SCHEMA', allVars.get('SCHEMA'))
                stringParam('MARKER_SCHEMA', allVars.get('MARKER_SCHEMA'))
                stringParam('CREDENTIALS', allVars.get('CREDENTIALS'))
            }
            environmentVariables {
                env('OPSGENIE_HEARTBEAT_NAME', allVars.get('OPSGENIE_HEARTBEAT_NAME'))
                env('OPSGENIE_HEARTBEAT_DURATION_NUM', allVars.get('OPSGENIE_HEARTBEAT_DURATION_NUM'))
                env('OPSGENIE_HEARTBEAT_DURATION_UNIT', allVars.get('OPSGENIE_HEARTBEAT_DURATION_UNIT'))
            }
            wrappers common_wrappers(allVars)
            wrappers {
                credentialsBinding {
                    string('OPSGENIE_HEARTBEAT_CONFIG_KEY', 'opsgenie_heartbeat_config_key')
                }
            }
            steps {
                shell(dslFactory.readFileFromWorkspace('dataeng/resources/opsgenie-enable-heartbeat.sh'))
                shell(dslFactory.readFileFromWorkspace('dataeng/resources/load-warehouse-vertica.sh'))
                shell(dslFactory.readFileFromWorkspace('dataeng/resources/opsgenie-disable-heartbeat.sh'))
            }
        }
    }

    public static def snowflake_job = { dslFactory, allVars ->
        dslFactory.job("load-warehouse-snowflake") {
            logRotator common_log_rotator(allVars)
            parameters common_parameters(allVars)
            parameters to_date_interval_parameter(allVars)
            parameters {
                stringParam('WAREHOUSE', allVars.get('WAREHOUSE'))
                stringParam('ROLE', allVars.get('ROLE'))
                stringParam('DATABASE', allVars.get('DATABASE'))
                stringParam('SCHEMA', allVars.get('SCHEMA'))
                stringParam('SCRATCH_SCHEMA', allVars.get('SCRATCH_SCHEMA'))
                stringParam('CREDENTIALS', allVars.get('CREDENTIALS'))
            }
            multiscm common_multiscm(allVars)
            wrappers common_wrappers(allVars)
            publishers common_publishers(allVars)
            steps {
                shell(dslFactory.readFileFromWorkspace('dataeng/resources/load-warehouse-snowflake.sh'))
            }
        }
    }

}

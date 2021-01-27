package analytics
import static org.edx.jenkins.dsl.AnalyticsConstants.common_multiscm
import static org.edx.jenkins.dsl.AnalyticsConstants.common_parameters
import static org.edx.jenkins.dsl.AnalyticsConstants.common_log_rotator
import static org.edx.jenkins.dsl.AnalyticsConstants.common_wrappers
import static org.edx.jenkins.dsl.AnalyticsConstants.common_publishers
import static org.edx.jenkins.dsl.AnalyticsConstants.common_triggers

class DatabaseExportCoursewareStudentmodule {
    public static def job = { dslFactory, allVars ->
        allVars.get('ENVIRONMENTS').each { environment, env_config ->
            dslFactory.job("database-export-courseware-studentmodule-$environment") {
                // As part of the MySQL upgrade for the LMS, we need to disable the jobs that
                // interface with the LMS read replica.
                // TODO: once the upgrade is complete for both prod and edge environments, remove this line
                disabled(env_config.get('JOB_DISABLED'))
                logRotator common_log_rotator(allVars, env_config)
                multiscm common_multiscm(allVars)
                triggers common_triggers(allVars, env_config)
                publishers common_publishers(allVars)
                parameters {
                    stringParam('BASE_OUTPUT_URL', env_config.get('BASE_OUTPUT_URL', allVars.get('BASE_OUTPUT_URL')), '')
                    stringParam('OUTPUT_DIR', env_config.get('OUTPUT_DIR', allVars.get('OUTPUT_DIR')), '')
                    stringParam('OUTPUT_SUFFIX', env_config.get('OUTPUT_SUFFIX', allVars.get('OUTPUT_SUFFIX')), '')
                    stringParam('CREDENTIALS', env_config.get('CREDENTIALS', allVars.get('CREDENTIALS')), '')
                }
                parameters common_parameters(allVars, env_config)
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
                    shell(dslFactory.readFileFromWorkspace('dataeng/resources/database-export-courseware-studentmodule.sh'))
                    shell(dslFactory.readFileFromWorkspace('dataeng/resources/opsgenie-disable-heartbeat.sh'))
                }
            }
        }
    }
}

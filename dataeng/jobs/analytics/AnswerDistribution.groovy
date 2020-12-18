package analytics
import static org.edx.jenkins.dsl.AnalyticsConstants.common_multiscm
import static org.edx.jenkins.dsl.AnalyticsConstants.common_parameters
import static org.edx.jenkins.dsl.AnalyticsConstants.common_log_rotator
import static org.edx.jenkins.dsl.AnalyticsConstants.common_wrappers
import static org.edx.jenkins.dsl.AnalyticsConstants.common_publishers
import static org.edx.jenkins.dsl.AnalyticsConstants.common_triggers

class AnswerDistribution {
    public static def job = { dslFactory, allVars ->
        allVars.get('ENVIRONMENTS').each { environment, env_config ->
            dslFactory.job("answer-distribution-$environment") {
                logRotator common_log_rotator(allVars, env_config)
                multiscm common_multiscm(allVars)
                triggers common_triggers(allVars, env_config)
                publishers common_publishers(allVars)
                parameters {
                    stringParam('SOURCES', env_config.get('SOURCES', allVars.get('SOURCES')), '')
                    stringParam('DESTINATION_PREFIX', env_config.get('DESTINATION_PREFIX', allVars.get('DESTINATION_PREFIX')), '')
                    stringParam('OUTPUT_URL', env_config.get('OUTPUT_URL', allVars.get('OUTPUT_URL')), '')
                    stringParam('CREDENTIALS', env_config.get('CREDENTIALS', allVars.get('CREDENTIALS')), '')
                    stringParam('LIB_JAR', env_config.get('LIB_JAR', allVars.get('LIB_JAR')), '')
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
                    shell(dslFactory.readFileFromWorkspace('dataeng/resources/answer-distribution.sh'))
                    shell(dslFactory.readFileFromWorkspace('dataeng/resources/opsgenie-disable-heartbeat.sh'))
                }
            }
        }
    }
}

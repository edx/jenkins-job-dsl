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
                logRotator common_log_rotator(allVars, env_config)
                parameters {
                    stringParam('BASE_OUTPUT_URL', env_config.get('BASE_OUTPUT_URL', allVars.get('BASE_OUTPUT_URL')), '')
                    stringParam('OUTPUT_DIR', env_config.get('OUTPUT_DIR', allVars.get('OUTPUT_DIR')), '')
                    stringParam('OUTPUT_SUFFIX', env_config.get('OUTPUT_SUFFIX', allVars.get('OUTPUT_SUFFIX')), '')
                    stringParam('CREDENTIALS', env_config.get('CREDENTIALS', allVars.get('CREDENTIALS')), '')
                }
                parameters common_parameters(allVars, env_config)
                multiscm common_multiscm(allVars)
                triggers common_triggers(allVars, env_config)
                wrappers common_wrappers(allVars)
                publishers common_publishers(allVars)
                steps {
                    shell(dslFactory.readFileFromWorkspace('dataeng/resources/database-export-courseware-studentmodule.sh'))
                    if (env_config.get('OPSGENIE_HEARTBEAT_NAME') && allVars.get('OPSGENIE_HEARTBEAT_KEY')){
                        shell("curl -X GET 'https://api.opsgenie.com/v2/heartbeats/" + env_config.get('OPSGENIE_HEARTBEAT_NAME') + "/ping' -H 'Authorization: GenieKey " + allVars.get('OPSGENIE_HEARTBEAT_KEY') + "'")
                    }
               }
            }
        }
    }
}

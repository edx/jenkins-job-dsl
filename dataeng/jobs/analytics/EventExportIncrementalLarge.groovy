package analytics
import static org.edx.jenkins.dsl.AnalyticsConstants.common_multiscm
import static org.edx.jenkins.dsl.AnalyticsConstants.secure_scm
import static org.edx.jenkins.dsl.AnalyticsConstants.data_czar_keys_scm
import static org.edx.jenkins.dsl.AnalyticsConstants.common_parameters
import static org.edx.jenkins.dsl.AnalyticsConstants.from_date_interval_parameter
import static org.edx.jenkins.dsl.AnalyticsConstants.to_date_interval_parameter
import static org.edx.jenkins.dsl.AnalyticsConstants.common_log_rotator
import static org.edx.jenkins.dsl.AnalyticsConstants.common_wrappers
import static org.edx.jenkins.dsl.AnalyticsConstants.common_publishers
import static org.edx.jenkins.dsl.AnalyticsConstants.common_triggers
import static org.edx.jenkins.dsl.AnalyticsConstants.common_authorization

class EventExportIncrementalLarge {
    public static def job = { dslFactory, allVars ->
        allVars.get('ENVIRONMENTS').each { environment, env_config ->
            dslFactory.job("event-export-incremental-large-$environment") {
                description('Job which groups tracking events by org and exports them to S3 for research purposes.')
                disabled(env_config.get('DISABLED', false))
                authorization common_authorization(allVars)
                logRotator common_log_rotator(allVars, env_config)
                parameters common_parameters(allVars, env_config)
                parameters from_date_interval_parameter(allVars)
                parameters to_date_interval_parameter(allVars)
                parameters {
                    stringParam('SOURCE', env_config.get('EVENT_LOGS_SOURCE'), '')
                    stringParam('OUTPUT_ROOT', allVars.get('OUTPUT_ROOT'))
                    stringParam('EXPORTER_CONFIG', allVars.get('EXPORTER_CONFIG'), 'Exporter configuration relative to analytics-secure/analytics-exporter')
                    stringParam('ONLY_ORGS', allVars.get('ONLY_ORGS'), "i.e. --org-id [\\\"FooX\\\",\\\"BarX\\\"]")
                    stringParam('DATA_CZAR_KEYS_BRANCH', allVars.get('DATA_CZAR_KEYS_BRANCH'), '')
                    stringParam('ENVIRONMENT', env_config.get('ENVIRONMENT'), '')
                }
                multiscm common_multiscm(allVars) >> secure_scm(allVars) >> data_czar_keys_scm(allVars)

                triggers common_triggers(allVars, env_config)
                wrappers common_wrappers(allVars)
                publishers common_publishers(allVars)
                steps {
                    shell(dslFactory.readFileFromWorkspace("dataeng/resources/event-export-incremental.sh"))
                }
            }
        }
    }
}

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

class EventExportIncremental {
    public static def job = { dslFactory, allVars ->
        allVars.get('ENVIRONMENTS').each { environment, env_config ->
            dslFactory.job("event-export-incremental-$environment") {
                logRotator common_log_rotator(allVars, env_config)
                parameters common_parameters(allVars, env_config)
                parameters from_date_interval_parameter(allVars)
                parameters to_date_interval_parameter(allVars)
                parameters {
                    stringParam('OUTPUT_ROOT', allVars.get('OUTPUT_ROOT'))
                    stringParam('EXPORTER_CONFIG', 'config.yaml', 'Exporter configuration relative to analytics-secure/analytics-exporter')
                    stringParam('ONLY_ORGS', '', "i.e. --org-id [\"FooX\",\"BarX\"]")
                    stringParam('DATA_CZAR_KEYS_BRANCH', 'master', '')
                    stringParam('ENVIRONMENT', env_config.get('ENVIRONMENT_SUBDIRECTORY'), '')
                }
                multiscm common_multiscm(allVars) >> secure_scm(allVars) >> data_czar_keys_scm(allVars)

                triggers common_triggers(allVars, env_config)
                wrappers common_wrappers(allVars)
                publishers common_publishers(allVars)
                publishers {
                    postBuildTask {
                        task('org with Errors=', 'exit 1', true)
                    }
                }
                steps {
                    shell(dslFactory.readFileFromWorkspace('dataeng/resources/event-export-incremental.sh'))
                    if (env_config.get('SNITCH')) {
                        shell('curl https://nosnch.in/' + allVars.get('SNITCH'))
                    }
                }
            }
        }
    }
}

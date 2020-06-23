package analytics
import static org.edx.jenkins.dsl.AnalyticsConstants.common_multiscm
import static org.edx.jenkins.dsl.AnalyticsConstants.common_parameters
import static org.edx.jenkins.dsl.AnalyticsConstants.from_date_interval_parameter
import static org.edx.jenkins.dsl.AnalyticsConstants.to_date_interval_parameter
import static org.edx.jenkins.dsl.AnalyticsConstants.common_log_rotator
import static org.edx.jenkins.dsl.AnalyticsConstants.common_wrappers
import static org.edx.jenkins.dsl.AnalyticsConstants.common_publishers
import static org.edx.jenkins.dsl.AnalyticsConstants.common_triggers

class LoadEvents {
    public static def load_events_to_s3_job = { dslFactory, allVars ->
        dslFactory.job("load-events-to-s3") {
            logRotator common_log_rotator(allVars)
            parameters common_parameters(allVars)
            parameters from_date_interval_parameter(allVars)
            parameters to_date_interval_parameter(allVars)
            parameters {
                stringParam('SOURCE', allVars.get('PRODUCTION_EVENT_LOGS_SOURCE'), '')
                stringParam('OUTPUT_URL', allVars.get('OUTPUT_URL'), '')
                stringParam('CREDENTIALS', allVars.get('CREDENTIALS'), '')
                stringParam('EVENTS_LIST', allVars.get('EVENTS_LIST'), '')
                stringParam('EVENT_RECORD_TYPE', allVars.get('EVENT_RECORD_TYPE', ''), '')
            }
            multiscm common_multiscm(allVars)
            triggers common_triggers(allVars)
            wrappers common_wrappers(allVars)
            publishers common_publishers(allVars)
            publishers {
                downstreamParameterized {
                    trigger('load-events-to-vertica') {
                        condition('SUCCESS')
                        parameters {
                            predefinedProp('CLUSTER_NAME', 'LoadEventsToVertica_$TO_DATE')
                            predefinedProp('NUM_TASK_INSTANCES', '0')
                            predefinedProp('TASKS_BRANCH', '$TASKS_BRANCH')
                            predefinedProp('CONF_BRANCH', '$CONF_BRANCH')
                            predefinedProp('OUTPUT_URL', '$OUTPUT_URL')
                            predefinedProp('CREDENTIALS', '$CREDENTIALS')
                            predefinedProp('TASK_USER', '$TASK_USER')
                            predefinedProp('SECURE_REPO', '$SECURE_REPO')
                            predefinedProp('SECURE_BRANCH', '$SECURE_BRANCH')
                            predefinedProp('SECURE_CONFIG', '$SECURE_CONFIG')
                            predefinedProp('NOTIFY', '$NOTIFY')
                            predefinedProp('FROM_DATE', '$FROM_DATE')
                            predefinedProp('TO_DATE', '$TO_DATE')
                            predefinedProp('EVENTS_LIST', '$EVENTS_LIST')
                        }
                    }
                }
            }
            steps {
                shell(dslFactory.readFileFromWorkspace('dataeng/resources/load-events-to-s3.sh'))
            }
        }
    }

    public static def load_events_to_vertica_job = { dslFactory, allVars ->
        dslFactory.job("load-events-to-vertica") {
            logRotator common_log_rotator(allVars)
            parameters common_parameters(allVars)
            parameters from_date_interval_parameter(allVars)
            parameters to_date_interval_parameter(allVars)
            parameters {
                stringParam('OUTPUT_URL', allVars.get('OUTPUT_URL'))
                stringParam('CREDENTIALS', allVars.get('CREDENTIALS'))
                stringParam('EVENTS_LIST', allVars.get('EVENTS_LIST'))
                stringParam('SCHEMA', allVars.get('SCHEMA'))
            }
            multiscm common_multiscm(allVars)
            wrappers common_wrappers(allVars)
            publishers common_publishers(allVars)
            steps {
                shell(dslFactory.readFileFromWorkspace('dataeng/resources/load-events-to-vertica.sh'))
            }
        }
    }

}

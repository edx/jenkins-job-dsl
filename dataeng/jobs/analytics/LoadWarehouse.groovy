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
            logRotator common_log_rotator(allVars)
            parameters common_parameters(allVars)
            parameters to_date_interval_parameter(allVars)
            parameters {
                stringParam('SCHEMA', allVars.get('SCHEMA'))
                stringParam('MARKER_SCHEMA', allVars.get('MARKER_SCHEMA'))
                stringParam('CREDENTIALS', allVars.get('CREDENTIALS'))
            }
            multiscm common_multiscm(allVars)
            triggers common_triggers(allVars)
            wrappers common_wrappers(allVars)
            publishers common_publishers(allVars)
            publishers {
                downstream("load-warehouse-bigquery", 'SUCCESS')
            }
            steps {
                shell(dslFactory.readFileFromWorkspace('dataeng/resources/load-warehouse-vertica.sh'))
                if (allVars.get('SNITCH')) {
                    shell('curl https://nosnch.in/' + allVars.get('SNITCH'))
                }
            }
        }
    }

    public static def bigquery_job = { dslFactory, allVars ->
        dslFactory.job("load-warehouse-bigquery") {
            logRotator common_log_rotator(allVars)
            parameters common_parameters(allVars)
            parameters to_date_interval_parameter(allVars)
            parameters {
                stringParam('DATASET', allVars.get('SCHEMA'))
                stringParam('CREDENTIALS', allVars.get('CREDENTIALS'))
            }
            multiscm common_multiscm(allVars)
            wrappers common_wrappers(allVars)
            publishers common_publishers(allVars)
            steps {
                shell(dslFactory.readFileFromWorkspace('dataeng/resources/load-warehouse-bigquery.sh'))
            }
        }
    }
}


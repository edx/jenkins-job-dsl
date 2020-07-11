package analytics
import static org.edx.jenkins.dsl.AnalyticsConstants.common_multiscm
import static org.edx.jenkins.dsl.AnalyticsConstants.common_parameters
import static org.edx.jenkins.dsl.AnalyticsConstants.to_date_interval_parameter
import static org.edx.jenkins.dsl.AnalyticsConstants.common_log_rotator
import static org.edx.jenkins.dsl.AnalyticsConstants.common_wrappers
import static org.edx.jenkins.dsl.AnalyticsConstants.common_publishers
import static org.edx.jenkins.dsl.AnalyticsConstants.common_triggers

class BigqueryReplicaImport {
    public static def job = { dslFactory, allVars ->
        allVars.get('BQ_READ_REPLICA_IMPORTS').each { db, db_config ->
            dslFactory.job("bigquery-${db.toLowerCase()}-read-replica-import") {

                // BigQuery deprecation
                disabled(true)

                logRotator common_log_rotator(allVars)
                parameters common_parameters(allVars, db_config)
                parameters {
                    stringParam('DATASET', db_config.get('DATASET'), 'Name of BigQuery dataset to write to.')
                    stringParam('DB_CREDENTIALS', db_config.get('DB_CREDENTIALS'), 'Credentials for reading from MySQL database.')
                    stringParam('DATABASE', db_config.get('DATABASE'), 'Name of MySQL database to copy from.')
                    stringParam('EXCLUDE_FIELD', db_config.get('EXCLUDE_FIELD'), 'List of regular expressions matching field names that should not be copied.')
                    stringParam('EXCLUDE', db_config.get('EXCLUDE'), 'List of regular expressions matching table names that should not be copied.')
                    stringParam('CREDENTIALS', allVars.get('CREDENTIALS'), 'Credentials for writing to BigQuery project.')
                }
                parameters to_date_interval_parameter(allVars)
                multiscm common_multiscm(allVars)
                wrappers common_wrappers(allVars)
                publishers common_publishers(allVars)
                triggers common_triggers(allVars, db_config)
                steps {
                    shell(dslFactory.readFileFromWorkspace('dataeng/resources/bigquery-replica-import.sh'))
                }
            }
        }
    }
}

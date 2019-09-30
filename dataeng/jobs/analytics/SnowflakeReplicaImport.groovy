package analytics
import static org.edx.jenkins.dsl.AnalyticsConstants.common_multiscm
import static org.edx.jenkins.dsl.AnalyticsConstants.common_parameters
import static org.edx.jenkins.dsl.AnalyticsConstants.to_date_interval_parameter
import static org.edx.jenkins.dsl.AnalyticsConstants.common_log_rotator
import static org.edx.jenkins.dsl.AnalyticsConstants.common_wrappers
import static org.edx.jenkins.dsl.AnalyticsConstants.common_publishers
import static org.edx.jenkins.dsl.AnalyticsConstants.common_triggers

class SnowflakeReplicaImport {
    public static def job = { dslFactory, allVars ->
        allVars.get('SNOWFLAKE_READ_REPLICA_IMPORTS').each { db, db_config ->
            dslFactory.job("snowflake-${db.toLowerCase()}-read-replica-import") {
                logRotator common_log_rotator(allVars)
                parameters common_parameters(allVars, db_config)
                parameters {
                    stringParam('RUN_DATE', db_config.get('RUN_DATE', allVars.get('RUN_DATE', 'today')), 'Run date for the job. A string that can be parsed by the GNU coreutils "date" utility.')
                    stringParam('OVERWRITE', db_config.get('OVERWRITE', allVars.get('OVERWRITE')), 'Set to: --overwrite if you want to enable overwrite.')
                    stringParam('SNOWFLAKE_CREDENTIALS', db_config.get('SNOWFLAKE_CREDENTIALS', allVars.get('SNOWFLAKE_CREDENTIALS')), 'The path to the Snowflake credentials file.')
                    stringParam('WAREHOUSE', db_config.get('WAREHOUSE', allVars.get('WAREHOUSE')), '')
                    stringParam('ROLE', db_config.get('ROLE', allVars.get('ROLE')))
                    stringParam('SNOWFLAKE_DATABASE', db_config.get('SNOWFLAKE_DATABASE', allVars.get('SNOWFLAKE_DATABASE')))
                    stringParam('SCHEMA', db_config.get('SCHEMA', allVars.get('SCHEMA')), 'Schema')
                    stringParam('SCRATCH_SCHEMA', schema_config.get('SCRATCH_SCHEMA', allVars.get('SCRATCH_SCHEMA')), 'Scratch schema name - temporary loading location.')
                    stringParam('DB_CREDENTIALS', db_config.get('DB_CREDENTIALS'), 'Credentials for reading from MySQL database.')
                    stringParam('DATABASE', db_config.get('DATABASE'), 'Name of MySQL database to copy from.')
                    stringParam('EXCLUDE_FIELD', db_config.get('EXCLUDE_FIELD'), 'List of regular expressions matching field names that should not be copied.')
                    stringParam('EXCLUDE', db_config.get('EXCLUDE'), 'List of regular expressions matching table names that should not be copied.')
                }
                multiscm common_multiscm(allVars)
                wrappers common_wrappers(allVars)
                publishers common_publishers(allVars)
                steps {
                    shell(dslFactory.readFileFromWorkspace('dataeng/resources/snowflake-replica-import.sh'))
                }
            }
        }
    }
}

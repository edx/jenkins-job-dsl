package analytics
import static org.edx.jenkins.dsl.AnalyticsConstants.common_multiscm
import static org.edx.jenkins.dsl.AnalyticsConstants.common_parameters
import static org.edx.jenkins.dsl.AnalyticsConstants.common_log_rotator
import static org.edx.jenkins.dsl.AnalyticsConstants.common_wrappers
import static org.edx.jenkins.dsl.AnalyticsConstants.common_publishers

class LoadVerticaSchemaToSnowflake {
    public static def job = { dslFactory, allVars ->
        allVars.get('VERTICA_SCHEMAS').each { schema, schema_config ->
            dslFactory.job("load-vertica-schema-to-snowflake-$schema") {
                logRotator common_log_rotator(allVars, schema_config)
                parameters common_parameters(allVars, schema_config)
                parameters {
                    stringParam('RUN_DATE', schema_config.get('RUN_DATE', allVars.get('RUN_DATE', 'today')), 'Run date for the job. A string that can be parsed by the GNU coreutils "date" utility.')
                    stringParam('OVERWRITE', schema_config.get('OVERWRITE', allVars.get('OVERWRITE')), 'Set to: --overwrite if you want to enable overwrite.')
                    stringParam('SNOWFLAKE_CREDENTIALS', schema_config.get('SNOWFLAKE_CREDENTIALS', allVars.get('SNOWFLAKE_CREDENTIALS')), 'The path to the Snowflake credentials file.')
                    stringParam('WAREHOUSE', schema_config.get('WAREHOUSE', allVars.get('WAREHOUSE')), '')
                    stringParam('ROLE', schema_config.get('ROLE', allVars.get('ROLE')))
                    stringParam('DATABASE', schema_config.get('DATABASE', allVars.get('DATABASE')))
                    stringParam('SCHEMA', schema_config.get('SCHEMA', allVars.get('SCHEMA')), 'Schema')
                    stringParam('SCRATCH_SCHEMA', schema_config.get('SCRATCH_SCHEMA', allVars.get('SCRATCH_SCHEMA')), 'ScratchSchema')
                    stringParam('RUN_ID', schema_config.get('RUN_ID', allVars.get('RUN_ID')))
                    stringParam('VERTICA_SCHEMA_NAME', schema_config.get('VERTICA_SCHEMA_NAME', allVars.get('VERTICA_SCHEMA_NAME')), 'Vertica Schema')
                    stringParam('VERTICA_CREDENTIALS', schema_config.get('VERTICA_CREDENTIALS', allVars.get('VERTICA_CREDENTIALS')), 'The path to the Vertica credentials file.')
                    stringParam('VERTICA_WAREHOUSE_NAME', schema_config.get('VERTICA_WAREHOUSE_NAME', allVars.get('VERTICA_WAREHOUSE_NAME')), '')
                    stringParam('EXCLUDE', schema_config.get('EXCLUDE', allVars.get('EXCLUDE')), 'as an example: --exclude [\"f_user_activity\",\"d_user_course_certificate\",\"d_user_course\"]')
                }
                multiscm common_multiscm(allVars)
                wrappers common_wrappers(allVars)
                publishers common_publishers(allVars)
                steps {
                    shell(dslFactory.readFileFromWorkspace('dataeng/resources/load-vertica-schema-to-snowflake.sh'))
                }
            }
        }
    }
}

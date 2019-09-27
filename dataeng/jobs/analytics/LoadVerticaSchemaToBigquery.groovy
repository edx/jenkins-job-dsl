package analytics
import static org.edx.jenkins.dsl.AnalyticsConstants.common_multiscm
import static org.edx.jenkins.dsl.AnalyticsConstants.common_parameters
import static org.edx.jenkins.dsl.AnalyticsConstants.common_log_rotator
import static org.edx.jenkins.dsl.AnalyticsConstants.common_wrappers
import static org.edx.jenkins.dsl.AnalyticsConstants.common_publishers

class LoadVerticaSchemaToBigquery {
    public static def job = { dslFactory, allVars ->
        allVars.get('VERTICA_SCHEMAS').each { schema, schema_config ->
            dslFactory.job("load-vertica-schema-to-bigquery-$schema") {
                logRotator common_log_rotator(allVars, schema_config)
                parameters common_parameters(allVars, schema_config)
                parameters {
                    stringParam('RUN_DATE', schema_config.get('RUN_DATE', allVars.get('RUN_DATE', 'today')), 'Run date for the job. A string that can be parsed by the GNU coreutils "date" utility.')
                    stringParam('OVERWRITE', schema_config.get('OVERWRITE', allVars.get('OVERWRITE')), 'Set to: --overwrite if you want to enable overwrite.')
                    stringParam('GCP_CREDENTIALS', schema_config.get('GCP_CREDENTIALS', allVars.get('GCP_CREDENTIALS')), 'The path to the GCP credentials file.')
                    stringParam('VERTICA_SCHEMA_NAME', schema_config.get('VERTICA_SCHEMA_NAME', allVars.get('VERTICA_SCHEMA_NAME')), 'Vertica Schema')
                    stringParam('VERTICA_CREDENTIALS', schema_config.get('VERTICA_CREDENTIALS', allVars.get('VERTICA_CREDENTIALS')), 'The path to the Vertica credentials file.')
                    stringParam('EXCLUDE', schema_config.get('EXCLUDE', allVars.get('EXCLUDE')), 'as an example: --exclude [\"f_user_activity\",\"d_user_course_certificate\",\"d_user_course\"]')
                }
                multiscm common_multiscm(allVars)
                wrappers common_wrappers(allVars)
                publishers common_publishers(allVars)
                steps {
                    shell(dslFactory.readFileFromWorkspace('dataeng/resources/load-vertica-schema-to-bigquery.sh'))
                }
            }
        }
    }
}

package analytics
import static org.edx.jenkins.dsl.AnalyticsConstants.common_multiscm
import static org.edx.jenkins.dsl.AnalyticsConstants.common_parameters
import static org.edx.jenkins.dsl.AnalyticsConstants.common_log_rotator
import static org.edx.jenkins.dsl.AnalyticsConstants.common_wrappers
import static org.edx.jenkins.dsl.AnalyticsConstants.common_publishers
import static org.edx.jenkins.dsl.AnalyticsConstants.common_triggers

class VerticaReplicaImportFromS3 {
    public static def job = { dslFactory, allVars ->
        allVars.get('VERTICA_READ_REPLICA_IMPORTS').each { db, db_config ->
            dslFactory.job("vertica-${db.toLowerCase()}-read-replica-import-from-s3") {
                logRotator common_log_rotator(allVars)
                parameters common_parameters(allVars, db_config)
                parameters {
                    stringParam('RUN_DATE', db_config.get('RUN_DATE', allVars.get('RUN_DATE', 'today')), 'Run date for the job. A string that can be parsed by the GNU coreutils "date" utility.')
                    stringParam('SCHEMA', db_config.get('SCHEMA'), 'Name of Vertica schema to write to.')
                    stringParam('MARKER_SCHEMA', db_config.get('MARKER_SCHEMA'))
                    stringParam('DATABASE', db_config.get('DATABASE'), 'Name of MySQL database to copy from.')
                    stringParam('INCLUDE', db_config.get('INCLUDE'),
                      'List of regular expressions matching table names that should be copied.' + $/

 The default for INCLUDE is "". But if one wants to set it, one needs to set "--include (list-of-patterns)".
 The flag ("--include") needs to be included in the parameter value.
 Also note that the quotes need to be escaped in patterns and they don't include any spaces.

 Ex:
   --include [\"auth_*\"]
 /$
                    )
                    stringParam('EXCLUDE', db_config.get('EXCLUDE'),
                      'List of regular expressions matching table names that should not be copied.' + $/

 The default for EXCLUDE is "". But if one wants to set it, one needs to set "--exclude (list-of-patterns)".
 The flag ("--exclude") needs to be included in the parameter value.
 Also note that the quotes need to be escaped in patterns and they don't include any spaces.

 Ex:
   --exclude [\"auth_*\"]
 /$
                    )
                    stringParam('CREDENTIALS', allVars.get('CREDENTIALS'), 'Credentials for writing to vertica.')
                }
                multiscm common_multiscm(allVars)
                wrappers common_wrappers(allVars)
                publishers common_publishers(allVars)
                steps {
                    shell(dslFactory.readFileFromWorkspace('dataeng/resources/vertica-replica-import.sh'))
                    if (db_config.get('SNITCH')) {
                        shell('curl https://nosnch.in/' + db_config.get('SNITCH'))
                    }
                }
            }
        }
    }
}
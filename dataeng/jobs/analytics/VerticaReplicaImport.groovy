package analytics
import static org.edx.jenkins.dsl.AnalyticsConstants.common_multiscm
import static org.edx.jenkins.dsl.AnalyticsConstants.common_parameters
import static org.edx.jenkins.dsl.AnalyticsConstants.to_date_interval_parameter
import static org.edx.jenkins.dsl.AnalyticsConstants.common_log_rotator
import static org.edx.jenkins.dsl.AnalyticsConstants.common_wrappers
import static org.edx.jenkins.dsl.AnalyticsConstants.common_publishers
import static org.edx.jenkins.dsl.AnalyticsConstants.common_triggers

class VerticaReplicaImport {
    public static def job = { dslFactory, allVars ->
        allVars.get('VERTICA_READ_REPLICA_IMPORTS').each { db, db_config ->
            dslFactory.job("${db.toLowerCase()}-read-replica-import") {

                // DENG-317
                disabled(true)

                logRotator common_log_rotator(allVars)
                parameters common_parameters(allVars, db_config)
                parameters {
                    stringParam('SCHEMA', db_config.get('SCHEMA'), 'Name of Vertica schema to write to.')
                    stringParam('MARKER_SCHEMA', db_config.get('MARKER_SCHEMA'))
                    stringParam('DB_CREDENTIALS', db_config.get('DB_CREDENTIALS'), 'Credentials for reading from MySQL database.')
                    stringParam('DATABASE', db_config.get('DATABASE'), 'Name of MySQL database to copy from.')
                    stringParam('EXCLUDE_FIELD', db_config.get('EXCLUDE_FIELD'),
                      'List of regular expressions matching field names that should not be copied.' + $/

 The default for EXCLUDE_FIELD is "". But if one wants to set it, one needs to set "--exclude-field (list-of-patterns)".
 The flag ("--exclude-field") needs to be included in the parameter value.
 Also note that the quotes need to be escaped in patterns and they don't include any spaces.

 Ex:
 --exclude-field [\".*password.*\"]
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
                parameters to_date_interval_parameter(allVars)
                multiscm common_multiscm(allVars)
                wrappers common_wrappers(allVars)
                publishers common_publishers(allVars)
                publishers {
                    downstreamParameterized {
                        trigger("snowflake-${db.toLowerCase()}-read-replica-import") {
                            condition('SUCCESS')
                            parameters {
                                // The contents of this file are generated as part of the script in the build step.
                                propertiesFile('${WORKSPACE}/downstream.properties')
                            }
                        }
                    }
                }
                triggers common_triggers(allVars, db_config)
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

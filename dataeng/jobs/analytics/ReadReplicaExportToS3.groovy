package analytics
import static org.edx.jenkins.dsl.AnalyticsConstants.common_multiscm
import static org.edx.jenkins.dsl.AnalyticsConstants.common_parameters
import static org.edx.jenkins.dsl.AnalyticsConstants.to_date_interval_parameter
import static org.edx.jenkins.dsl.AnalyticsConstants.common_log_rotator
import static org.edx.jenkins.dsl.AnalyticsConstants.common_wrappers
import static org.edx.jenkins.dsl.AnalyticsConstants.common_publishers
import static org.edx.jenkins.dsl.AnalyticsConstants.common_triggers

class ReadReplicaExportToS3 {
    public static def job = { dslFactory, allVars ->
        allVars.get('READ_REPLICA_EXPORTS').each { db, db_config ->
            dslFactory.job("${db.toLowerCase()}-read-replica-export-to-s3") {
                logRotator common_log_rotator(allVars)
                parameters common_parameters(allVars, db_config)
                parameters {
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
                }
                parameters to_date_interval_parameter(allVars)
                environmentVariables {
                    env('OPSGENIE_HEARTBEAT_NAME', env_config.get('OPSGENIE_HEARTBEAT_NAME'))
                    env('OPSGENIE_HEARTBEAT_DURATION_NUM', env_config.get('OPSGENIE_HEARTBEAT_DURATION_NUM'))
                    env('OPSGENIE_HEARTBEAT_DURATION_UNIT', env_config.get('OPSGENIE_HEARTBEAT_DURATION_UNIT'))
                }
                multiscm common_multiscm(allVars)
                wrappers common_wrappers(allVars)
                wrappers {
                    credentialsBinding {
                        string('OPSGENIE_HEARTBEAT_CONFIG_KEY', 'opsgenie_heartbeat_config_key')
                    }
                }
                publishers common_publishers(allVars)
                publishers {
                    downstreamParameterized {
                        trigger("snowflake-${db.toLowerCase()}-read-replica-import-from-s3," +
                                "vertica-${db.toLowerCase()}-read-replica-import-from-s3"
                        ) {
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
                    shell(dslFactory.readFileFromWorkspace('dataeng/resources/opsgenie-enable-heartbeat.sh'))
                    shell(dslFactory.readFileFromWorkspace('dataeng/resources/read-replica-export.sh'))
                    shell(dslFactory.readFileFromWorkspace('dataeng/resources/opsgenie-disable-heartbeat.sh'))
                }
            }
        }
    }
}

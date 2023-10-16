package analytics
import static org.edx.jenkins.dsl.AnalyticsConstants.common_multiscm
import static org.edx.jenkins.dsl.AnalyticsConstants.common_parameters
import static org.edx.jenkins.dsl.AnalyticsConstants.common_log_rotator
import static org.edx.jenkins.dsl.AnalyticsConstants.common_wrappers
import static org.edx.jenkins.dsl.AnalyticsConstants.common_publishers

class SnowflakeReplicaImportFromS3 {
    public static def job = { dslFactory, allVars ->

        Map jobConfigs = [
            CREDENTIALS: [
                CLUSTER_NAME: 'ImportCredentialsReadReplicaSnowflakeFromS3',
                SCHEMA: 'CREDENTIALS_RAW',
                SCRATCH_SCHEMA: 'CREDENTIALS_SCRATCH_LOADING',
                DATABASE: 'credentials',
                INCLUDE: '',
                EXCLUDE: '',
            ],
            LMS: [
                CLUSTER_NAME: 'ImportLMSReadReplicaSnowflakeFromS3',
                SCHEMA: 'LMS_RAW',
                SCRATCH_SCHEMA: 'LMS_SCRATCH_LOADING',
                DATABASE: 'wwc',
                INCLUDE: '',
                EXCLUDE: '--exclude [\\"student_passwordhistory\\",\\".*courseware_studentmodule.*\\",\\"oauth.*\\",\\"shoppingcart_orderitem\\",\\"certificates_certificateinvalidation\\",\\"certificates_certificategenerationhistory\\",\\"enterprise_enterprisecustomerreportingconfiguration\\",\\"social_auth_partial\\"]',
            ],
            DISCOVERY: [
                CLUSTER_NAME: 'ImportDiscoveryReadReplicaSnowflakeFromS3',
                SCHEMA: 'DISCOVERY_RAW',
                SCRATCH_SCHEMA: 'DISCOVERY_SCRATCH_LOADING',
                DATABASE: 'discovery',
                INCLUDE: '',
                EXCLUDE: '--exclude [\\"auth_.*\\",\\"core_.*\\",\\"django_.*\\",\\"social_auth_.*\\",\\"waffle_.*\\"]',
            ],
            ECOMMERCE: [
                CLUSTER_NAME: 'ImportEcommerceReadReplicaSnowflakeFromS3',
                SCHEMA: 'ECOMMERCE_RAW',
                SCRATCH_SCHEMA: 'ECOMMERCE_SCRATCH_LOADING',
                DATABASE: 'ecommerce',
                INCLUDE: '',
                EXCLUDE: '',
            ],
        ]

        jobConfigs.each { appName, jobConfig ->
            dslFactory.job("snowflake-${appName.toLowerCase()}-read-replica-import-from-s3") {
                description('This imports Mysql Database from S3 to Snowflake Schema.')
                disabled(jobConfig.get('DISABLED', false))
                logRotator common_log_rotator(allVars)
                parameters common_parameters(allVars, jobConfig)
                parameters {
                    stringParam('RUN_DATE', 'today', 'Run date for the job. A string that can be parsed by the GNU coreutils "date" utility.')
                    stringParam('OVERWRITE', '--overwrite', 'Set to: --overwrite if you want to enable overwrite.')
                    stringParam('SNOWFLAKE_CREDENTIALS', allVars.get('SNOWFLAKE_CREDENTIALS'), 'The path to the Snowflake credentials file.')
                    stringParam('WAREHOUSE', 'LOADING_SQOOP', 'The Snowflake warehouse to use.')
                    stringParam('ROLE', 'PIPELINE_ETL_LOADER_ROLE', 'The Snowflake role to assume.')
                    stringParam('SNOWFLAKE_DATABASE', 'PROD', 'The destination database in Snowflake.')
                    stringParam('SCHEMA', jobConfig.get('SCHEMA'), 'The destination raw schema in Snowflake.')
                    stringParam('SCRATCH_SCHEMA', jobConfig.get('SCRATCH_SCHEMA'), 'Scratch schema name - temporary loading location in Snowflake.')
                    stringParam('DATABASE', jobConfig.get('DATABASE'), 'Name of MySQL database to copy from.')
                    stringParam('INCLUDE', jobConfig.get('INCLUDE', ''), 'List of regular expressions matching table names that should be copied.')
                    stringParam('EXCLUDE', jobConfig.get('EXCLUDE', ''), 'List of regular expressions matching table names that should not be copied.')
                    stringParam('SQOOP_START_TIME', '', 'The start time of the upstream sqoop load (in ISO timestamp format).')
                    booleanParam('FORCE', false, 'Force the job to run today, regardless of whether it is scheduled for today.')
                }
                environmentVariables {
                    env('APP_NAME', appName)
                }
                multiscm common_multiscm(allVars)
                wrappers common_wrappers(allVars)
                steps {
                    shell(dslFactory.readFileFromWorkspace('dataeng/resources/snowflake-replica-import.sh'))
                }
                publishers common_publishers(allVars)
                publishers {
                    downstreamParameterized {
                        trigger("snowflake-validate-stitch-$appName") {
                            condition('SUCCESS')
                            parameters {
                                // The contents of this file are generated as part of the script in the build step.
                                // The second function argument (the boolean) is failTriggerOnMissing, meaning we should
                                // not trigger the downstream validation job if the parameters file is missing.
                                propertiesFile('${WORKSPACE}/downstream.properties', true)
                            }
                        }
                    }
                }
            }
        }
    }
}

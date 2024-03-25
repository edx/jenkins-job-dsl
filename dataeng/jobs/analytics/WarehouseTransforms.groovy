package analytics
import static org.edx.jenkins.dsl.AnalyticsConstants.secure_scm
import static org.edx.jenkins.dsl.AnalyticsConstants.common_log_rotator
import static org.edx.jenkins.dsl.AnalyticsConstants.common_wrappers
import static org.edx.jenkins.dsl.AnalyticsConstants.common_publishers
import static org.edx.jenkins.dsl.AnalyticsConstants.common_triggers
import static org.edx.jenkins.dsl.AnalyticsConstants.secure_scm_parameters
import static org.edx.jenkins.dsl.AnalyticsConstants.common_authorization
import static org.edx.jenkins.dsl.AnalyticsConstants.opsgenie_heartbeat_publisher

class WarehouseTransforms{
    public static def job = { dslFactory, allVars ->
        allVars.get('ENVIRONMENTS').each { environment, env_config ->
            dslFactory.job("warehouse-transforms-$environment"){
                description(env_config.get('DESCRIPTION', ''))
                disabled(env_config.get('DISABLED', false))
                authorization common_authorization(env_config)
                logRotator common_log_rotator(allVars)
                parameters secure_scm_parameters(allVars)
                parameters {
                    stringParam('WAREHOUSE_TRANSFORMS_URL', allVars.get('WAREHOUSE_TRANSFORMS_URL'), 'URL for the Warehouse Transforms Repo.')
                    stringParam('WAREHOUSE_TRANSFORMS_BRANCH', allVars.get('WAREHOUSE_TRANSFORMS_BRANCH'), 'Branch of Warehouse Transforms to use.')
                    stringParam('MODEL_SELECTOR', env_config.get('MODEL_SELECTOR', allVars.get('MODEL_SELECTOR')), 'Model selector that will be run.  Often a tag selector.')
                    stringParam('DBT_PROJECT', env_config.get('DBT_PROJECT', allVars.get('DBT_PROJECT')), 'dbt project in warehouse-transforms to work on.')
                    stringParam('DBT_PROFILE', env_config.get('DBT_PROFILE', allVars.get('DBT_PROFILE')), 'dbt profile from analytics-secure to work on.')
                    stringParam('DBT_TARGET', env_config.get('DBT_TARGET', allVars.get('DBT_TARGET')), 'dbt target from analytics-secure to work on.')
                    stringParam('DBT_COMMAND', env_config.get('DBT_COMMAND', allVars.get('DBT_COMMAND', 'run')), 'dbt command to be executed.  Default is \'run\'.')
                    stringParam('SEED_SELECTOR', env_config.get('SEED_SELECTOR', allVars.get('SEED_SELECTOR', '*')), 'Model selector that will be used when running the seed step.  Defaults to "*".')
                    stringParam('SKIP_TESTS', env_config.get('SKIP_TESTS', 'false'), 'Set to \'true\' to skip all tests. All other values will allow tests to run given the other configuration values.')
                    stringParam('SKIP_SEED', env_config.get('SKIP_SEED', 'false'), 'Set to \'true\' to skip the dbt seed step. All other values will cause the seed step to be run.')
                    stringParam('TEST_SOURCES_FIRST', env_config.get('TEST_SOURCES_FIRST', 'true'), 'Set to \'true\' to perform source testing first (if SKIP_TESTS is false). All other values test sources post-run.')
                    booleanParam('CAUTIOUS_INDIRECT_SELECTION', env_config.get('CAUTIOUS_INDIRECT_SELECTION', false), 'Check this box if you want a test to run if and only if ALL the models associated with that test have been selected (see dbt docs for --indirect-selection=cautious).')
                    stringParam('PUSH_ARTIFACTS_TO_SNOWFLAKE', env_config.get('PUSH_ARTIFACTS_TO_SNOWFLAKE', 'false'), 'Set to \'true\' to push the run results file to Snowflake for telemetry. Avoid this on frequently-running jobs.')
                    stringParam('TEST_PARENT_MODELS_FIRST', env_config.get('TEST_PARENT_MODELS_FIRST', 'false'), 'Set to \'true\' to run the upstream models tests first.')
                    stringParam('NOTIFY', env_config.get('NOTIFY', allVars.get('NOTIFY','$PAGER_NOTIFY')), 'Space separated list of emails to send notifications to.')
                    booleanParam('FULL_REFRESH_INCREMENTALS', false, '[DANGEROUS] Supply the --full-refresh flag to the `dbt run` command, and use a larger warehouse. Use when you need to re-compute an incremental table from scratch.  Applies to ALL incrementals in this run.')
                }
                multiscm secure_scm(allVars) << {
                    git {
                        remote {
                            url('$WAREHOUSE_TRANSFORMS_URL')
                            branch('$WAREHOUSE_TRANSFORMS_BRANCH')
                            credentials('1')
                        }
                        extensions {
                            relativeTargetDirectory('warehouse-transforms')
                            pruneBranches()
                            cleanAfterCheckout()
                        }
                    }
                }
                triggers common_triggers(allVars, env_config)
                environmentVariables {
                    env('OPSGENIE_HEARTBEAT_NAME', env_config.get('OPSGENIE_HEARTBEAT_NAME'))
                    env('OPSGENIE_HEARTBEAT_DURATION_NUM', env_config.get('OPSGENIE_HEARTBEAT_DURATION_NUM'))
                    env('OPSGENIE_HEARTBEAT_DURATION_UNIT', env_config.get('OPSGENIE_HEARTBEAT_DURATION_UNIT'))
                }
                wrappers common_wrappers(allVars)
                wrappers {
                    colorizeOutput('xterm')
                }
                publishers common_publishers(allVars) << {
                    env_config.get('DOWNSTREAM_JOBS', []).each { downstream_job_name ->
                        downstream(downstream_job_name)
                    }
                }
                publishers opsgenie_heartbeat_publisher(allVars)
                wrappers {
                    credentialsBinding {
                        string('OPSGENIE_HEARTBEAT_CONFIG_KEY', 'opsgenie_heartbeat_config_key')
                        usernamePassword('ANALYTICS_VAULT_ROLE_ID', 'ANALYTICS_VAULT_SECRET_ID', 'analytics-vault');
                    }
                }
                steps {
                    shell(dslFactory.readFileFromWorkspace('dataeng/resources/secrets-manager-setup.sh'))
                    shell(dslFactory.readFileFromWorkspace('dataeng/resources/opsgenie-enable-heartbeat.sh'))
                    shell(dslFactory.readFileFromWorkspace('dataeng/resources/warehouse-transforms.sh'))
                }
            }
        }
    }
}


package analytics
import static org.edx.jenkins.dsl.AnalyticsConstants.common_log_rotator
import static org.edx.jenkins.dsl.AnalyticsConstants.common_wrappers
import static org.edx.jenkins.dsl.AnalyticsConstants.common_publishers
import static org.edx.jenkins.dsl.AnalyticsConstants.common_triggers
import static org.edx.jenkins.dsl.AnalyticsConstants.common_authorization
import static org.edx.jenkins.dsl.AnalyticsConstants.common_groovy_postbuild
import static org.edx.jenkins.dsl.AnalyticsConstants.common_datadog_build_end

class ModelTransfers{
    public static def job = { dslFactory, allVars ->
        allVars.get('ENVIRONMENTS').each { environment, env_config ->
            dslFactory.job("transfer-dbt-models-$environment"){
                authorization common_authorization(env_config)
                logRotator common_log_rotator(allVars)
                parameters {
                    stringParam('WAREHOUSE_TRANSFORMS_URL', allVars.get('WAREHOUSE_TRANSFORMS_URL'), 'URL for the Warehouse Transforms Repo.')
                    stringParam('WAREHOUSE_TRANSFORMS_BRANCH', allVars.get('WAREHOUSE_TRANSFORMS_BRANCH'), 'Branch of Warehouse Transforms to use.')
                    stringParam('DBT_PROJECT', env_config.get('DBT_PROJECT', allVars.get('DBT_PROJECT')), 'dbt project in warehouse-transforms to work on.')
                    stringParam('DBT_PROFILE', env_config.get('DBT_PROFILE', allVars.get('DBT_PROFILE')), 'dbt profile from analytics-secure to work on.')
                    stringParam('DBT_TARGET', env_config.get('DBT_TARGET', allVars.get('DBT_TARGET')), 'dbt target from analytics-secure to work on.')
                    stringParam('MODELS_TO_TRANSFER', env_config.get('MODELS_TO_TRANSFER'), 'Name of DBT models which should be transferred to S3 via a Snowflake stage.')
                    stringParam('NOTIFY', env_config.get('NOTIFY', allVars.get('NOTIFY','$PAGER_NOTIFY')), 'Space separated list of emails to send notifications to.')
                    stringParam('BUILD_STATUS')
                }
                multiscm {
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
                wrappers common_wrappers(allVars)
                wrappers {
                    colorizeOutput('xterm')
                }
                publishers common_datadog_build_end(dslFactory, allVars) << common_groovy_postbuild(dslFactory, allVars) << common_publishers(allVars)
                steps {
                    shell(dslFactory.readFileFromWorkspace('dataeng/resources/datadog_job_start.sh'))
                    shell(dslFactory.readFileFromWorkspace('dataeng/resources/secrets-manager-setup.sh'))
                    shell(dslFactory.readFileFromWorkspace('dataeng/resources/model-transfers.sh'))
                }
            }
        }
    }
}

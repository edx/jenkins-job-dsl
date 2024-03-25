package analytics
import static org.edx.jenkins.dsl.AnalyticsConstants.common_log_rotator
import static org.edx.jenkins.dsl.AnalyticsConstants.common_publishers
import static org.edx.jenkins.dsl.AnalyticsConstants.common_triggers
import static org.edx.jenkins.dsl.AnalyticsConstants.secure_scm
import static org.edx.jenkins.dsl.AnalyticsConstants.secure_scm_parameters

class SnowflakeSchemaBuilder {
    public static def job = { dslFactory, allVars ->
        dslFactory.job('snowflake-schema-builder') {
            logRotator common_log_rotator(allVars)
            parameters secure_scm_parameters(allVars)
            parameters {
                stringParam('WAREHOUSE_TRANSFORMS_URL', allVars.get('WAREHOUSE_TRANSFORMS_URL'), 'URL for the Warehouse Transforms Repo.')
                stringParam('WAREHOUSE_TRANSFORMS_BRANCH', allVars.get('WAREHOUSE_TRANSFORMS_BRANCH'), 'Branch of Warehouse Transforms to use.')
                stringParam('SOURCE_PROJECT', allVars.get('SOURCE_PROJECT'), 'The dbt project where the models will be generated and run, relative to the "projects" directory.')
                stringParam('DESTINATION_PROJECT', allVars.get('DESTINATION_PROJECT'), 'The dbt project that will use the generated sources, relative to the SOURCE_PROJECT.')
                stringParam('DBT_PROFILE', allVars.get('DBT_PROFILE'), 'dbt profile from analytics-secure to work on.')
                stringParam('DBT_TARGET', allVars.get('DBT_TARGET'), 'dbt target from analytics-secure to work on.')
                stringParam('NOTIFY', allVars.get('NOTIFY','$PAGER_NOTIFY'), 'Space separated list of emails to send notifications to.')
            }
            logRotator common_log_rotator(allVars)
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
            triggers common_triggers(allVars)
            wrappers {
                timestamps()
                credentialsBinding {
                    string('GITHUB_TOKEN', 'GHPRB_BOT_TOKEN');
                }
            }
            publishers common_publishers(allVars)
            steps {

                // This will create python 3.8 venv inside shell script instead of using shiningpanda
                shell(dslFactory.readFileFromWorkspace('dataeng/resources/secrets-manager-setup.sh'))
                shell(dslFactory.readFileFromWorkspace('dataeng/resources/snowflake-schema-builder.sh'))
            }
        }
    }
}

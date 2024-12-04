package analytics
import static org.edx.jenkins.dsl.AnalyticsConstants.common_log_rotator
import static org.edx.jenkins.dsl.AnalyticsConstants.common_wrappers
import static org.edx.jenkins.dsl.AnalyticsConstants.common_publishers
import static org.edx.jenkins.dsl.AnalyticsConstants.common_triggers
import static org.edx.jenkins.dsl.AnalyticsConstants.common_authorization
import static org.edx.jenkins.dsl.JenkinsPublicConstants.GHPRB_CANCEL_BUILDS_ON_UPDATE

class WarehouseTransformsCIManual{
    public static def job = { dslFactory, allVars ->
        dslFactory.job("warehouse-transforms-ci-manual"){
            authorization common_authorization(allVars)
            logRotator common_log_rotator(allVars)
            parameters {
                stringParam('WAREHOUSE_TRANSFORMS_URL', allVars.get('WAREHOUSE_TRANSFORMS_URL'), 'URL for the warehouse-transforms repository.')
                stringParam('WAREHOUSE_TRANSFORMS_BRANCH', '', 'Must specify branch of warehouse-transforms repository to use.')
                stringParam('GITHUB_PR_ID', '', 'Must specify Github Pull Request Id')
                stringParam('DBT_TARGET', allVars.get('DBT_TARGET'), 'DBT target from profiles.yml in analytics-secure.')
                stringParam('DBT_PROFILE', allVars.get('DBT_PROFILE'), 'DBT profile from profiles.yml in analytics-secure.')
                stringParam('DBT_PROJECT_PATH', allVars.get('DBT_PROJECT_PATH'), 'Path in warehouse-transforms to use as the dbt project, relative to "projects" (usually automated/applications or reporting).')
                stringParam('RUN_DBT_TESTS_ONLY', allVars.get('RUN_DBT_TESTS_ONLY'), 'Mark it as false if you want to run dbt models')
                stringParam('DBT_RUN_OPTIONS', allVars.get('DBT_RUN_OPTIONS'), 'Additional options to dbt run, such as --models for model selection. Details here: https://docs.getdbt.com/docs/model-selection-syntax')
                stringParam('DBT_RUN_EXCLUDE', allVars.get('DBT_RUN_EXCLUDE'), 'Additional options to dbt run, such as --exclude. Details here: https://docs.getdbt.com/docs/model-selection-syntax')
                stringParam('DBT_TEST_OPTIONS', allVars.get('DBT_TEST_OPTIONS'), 'Additional options to dbt test, such as --models for model selection. Details here: https://docs.getdbt.com/docs/model-selection-syntax')
                stringParam('DBT_TEST_EXCLUDE', allVars.get('DBT_TEST_EXCLUDE'), 'Additional options to dbt test, such as "--exclude <model>". Details here: https://docs.getdbt.com/docs/model-selection-syntax')
                stringParam('DB_NAME', allVars.get('DB_NAME'), 'Database name used to create output schema of dbt run/tests')
                stringParam('NOTIFY', allVars.get('NOTIFY'), 'Space separated list of emails to send notifications to.')
                stringParam('ANALYTICS_TOOLS_URL', allVars.get('ANALYTICS_TOOLS_URL'), 'URL for the analytics tools repo.')
                stringParam('ANALYTICS_TOOLS_BRANCH', allVars.get('ANALYTICS_TOOLS_BRANCH'), 'Branch of analytics tools repo to use.')
                stringParam('JENKINS_JOB_DSL_URL', allVars.get('JENKINS_JOB_DSL_URL'), 'URL for the jenkins-job-dsl repo.')
                stringParam('JENKINS_JOB_DSL_BRANCH', allVars.get('JENKINS_JOB_DSL_BRANCH'), 'Branch of jenkins-job-dsl repo to use.')

            }
            environmentVariables {
                env('KEY_PATH', allVars.get('KEY_PATH'))
                env('PASSPHRASE_PATH', allVars.get('PASSPHRASE_PATH'))
                env('USER', allVars.get('USER'))
                env('ACCOUNT', allVars.get('ACCOUNT'))
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
                git {
                    remote {
                        url('$ANALYTICS_TOOLS_URL')
                        branch('$ANALYTICS_TOOLS_BRANCH')
                        credentials('1')
                    }
                    extensions {
                        relativeTargetDirectory('analytics-tools')
                        pruneBranches()
                        cleanAfterCheckout()
                    }
                }
                git {
                    remote {
                        url('$JENKINS_JOB_DSL_URL')
                        branch('$JENKINS_JOB_DSL_BRANCH')
                        credentials('1')
                    }
                    extensions {
                        relativeTargetDirectory('jenkins-job-dsl')
                        pruneBranches()
                        cleanAfterCheckout()
                    }
                }
            }
            triggers common_triggers(allVars)
            publishers common_publishers(allVars)
            concurrentBuild(true)
            throttleConcurrentBuilds {
                maxTotal(5)
            }
            wrappers {
                colorizeOutput('xterm')
            }
            wrappers common_wrappers(allVars)
            steps {
                shell(dslFactory.readFileFromWorkspace('dataeng/resources/secrets-manager-setup.sh'))
                shell(dslFactory.readFileFromWorkspace('dataeng/resources/warehouse-transforms-ci-manual.sh'))
            }
        }
    }
}

package analytics
import static org.edx.jenkins.dsl.AnalyticsConstants.common_log_rotator
import static org.edx.jenkins.dsl.AnalyticsConstants.common_wrappers
import static org.edx.jenkins.dsl.AnalyticsConstants.common_publishers
import static org.edx.jenkins.dsl.AnalyticsConstants.common_triggers
import static org.edx.jenkins.dsl.AnalyticsConstants.common_authorization
import static org.edx.jenkins.dsl.AnalyticsConstants.slack_publisher
import static org.edx.jenkins.dsl.AnalyticsConstants.common_groovy_postbuild
import static org.edx.jenkins.dsl.AnalyticsConstants.common_datadog_build_end


class WarehouseTransformsCIMasterMerges{
    public static def job = { dslFactory, allVars ->
        dslFactory.job("warehouse-transforms-ci-poll-master"){
            authorization common_authorization(allVars)
            logRotator common_log_rotator(allVars)
            parameters {
                stringParam('WAREHOUSE_TRANSFORMS_URL', allVars.get('WAREHOUSE_TRANSFORMS_URL'), 'URL for the warehouse-transforms repository.')
                stringParam('WAREHOUSE_TRANSFORMS_BRANCH', allVars.get('WAREHOUSE_TRANSFORMS_BRANCH'), 'Branch of warehouse-transforms repository to use.')
                stringParam('DBT_TARGET', allVars.get('DBT_TARGET'), 'DBT target from profiles.yml in analytics-secure.')
                stringParam('DBT_PROFILE', allVars.get('DBT_PROFILE'), 'DBT profile from profiles.yml in analytics-secure.')
                stringParam('DBT_RUN_OPTIONS', allVars.get('DBT_RUN_OPTIONS'), 'Additional options to dbt run, such as --models for model selection. Details here: https://docs.getdbt.com/docs/model-selection-syntax')
                stringParam('DBT_RUN_EXCLUDE', allVars.get('DBT_RUN_EXCLUDE'), 'Additional options to dbt run, such as --exclude. Details here: https://docs.getdbt.com/docs/model-selection-syntax')
                stringParam('DBT_TEST_OPTIONS', allVars.get('DBT_TEST_OPTIONS'), 'Additional options to dbt test, such as --models for model selection. Details here: https://docs.getdbt.com/docs/model-selection-syntax')
                stringParam('DBT_TEST_EXCLUDE', allVars.get('DBT_TEST_EXCLUDE'), 'Additional options to dbt test, such as --exclude. Details here: https://docs.getdbt.com/docs/model-selection-syntax')
                stringParam('ANALYTICS_TOOLS_URL', allVars.get('ANALYTICS_TOOLS_URL'), 'URL for the analytics tools repo.')
                stringParam('ANALYTICS_TOOLS_BRANCH', allVars.get('ANALYTICS_TOOLS_BRANCH'), 'Branch of analytics tools repo to use.')
                stringParam('JENKINS_JOB_DSL_URL', allVars.get('JENKINS_JOB_DSL_URL'), 'URL for the jenkins-job-dsl repo.')
                stringParam('JENKINS_JOB_DSL_BRANCH', allVars.get('JENKINS_JOB_DSL_BRANCH'), 'Branch of jenkins-job-dsl repo to use.')
                stringParam('DB_NAME', allVars.get('DB_NAME'), 'Database name used to create output schema of dbt run/tests')
                stringParam('NOTIFY', allVars.get('NOTIFY','$PAGER_NOTIFY'), 'Space separated list of emails to send notifications to.')
            }
            scm {
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
            triggers {
                scm('H/10 * * * *')
            }

            wrappers {
                colorizeOutput('xterm')
            }
            wrappers common_wrappers(allVars)
        }


        dslFactory.job("warehouse-transforms-ci-master-merges"){
            authorization common_authorization(allVars)
            logRotator common_log_rotator(allVars)
            parameters {
                stringParam('WAREHOUSE_TRANSFORMS_URL', allVars.get('WAREHOUSE_TRANSFORMS_URL'), 'URL for the warehouse-transforms repository.')
                stringParam('WAREHOUSE_TRANSFORMS_BRANCH', allVars.get('WAREHOUSE_TRANSFORMS_BRANCH'), 'Branch of warehouse-transforms repository to use.')
                stringParam('DBT_TARGET', allVars.get('DBT_TARGET'), 'DBT target from profiles.yml in analytics-secure.')
                stringParam('DBT_PROFILE', allVars.get('DBT_PROFILE'), 'DBT profile from profiles.yml in analytics-secure.')
                stringParam('DBT_RUN_OPTIONS', allVars.get('DBT_RUN_OPTIONS'), 'Additional options to dbt run, such as --models for model selection. Details here: https://docs.getdbt.com/docs/model-selection-syntax')
                stringParam('DBT_RUN_EXCLUDE', allVars.get('DBT_RUN_EXCLUDE'), 'Additional options to dbt run, such as --exclude. Details here: https://docs.getdbt.com/docs/model-selection-syntax')
                stringParam('DBT_TEST_OPTIONS', allVars.get('DBT_TEST_OPTIONS'), 'Additional options to dbt test, such as --models for model selection. Details here: https://docs.getdbt.com/docs/model-selection-syntax')
                stringParam('DBT_TEST_EXCLUDE', allVars.get('DBT_TEST_EXCLUDE'), 'Additional options to dbt test, such as --exclude. Details here: https://docs.getdbt.com/docs/model-selection-syntax')
                stringParam('ANALYTICS_TOOLS_URL', allVars.get('ANALYTICS_TOOLS_URL'), 'URL for the analytics tools repo.')
                stringParam('ANALYTICS_TOOLS_BRANCH', allVars.get('ANALYTICS_TOOLS_BRANCH'), 'Branch of analytics tools repo to use.')
                stringParam('JENKINS_JOB_DSL_URL', allVars.get('JENKINS_JOB_DSL_URL'), 'URL for the jenkins-job-dsl repo.')
                stringParam('JENKINS_JOB_DSL_BRANCH', allVars.get('JENKINS_JOB_DSL_BRANCH'), 'Branch of jenkins-job-dsl repo to use.')
                stringParam('DB_NAME', allVars.get('DB_NAME'), 'Database name used to create output schema of dbt run/tests')
                stringParam('WITH_SNAPSHOT', allVars.get('WITH_SNAPSHOT'), 'When enabled (true) it runs dbt snapshot')
                stringParam('NOTIFY', allVars.get('NOTIFY'), 'Space separated list of emails to send notifications to.')
                stringParam('SLACK_NOTIFICATION_CHANNEL', allVars.get('SLACK_NOTIFICATION_CHANNEL'), 'Space separated list of slack channel name to send build failure notifications')
                stringParam('FAILURE_MESSAGE', allVars.get('FAILURE_MESSAGE'), 'Custom message for to send along with buid failure notification')
                stringParam('BUILD_STATUS')
            }
            environmentVariables {
                env('KEY_PATH', allVars.get('KEY_PATH'))
                env('PASSPHRASE_PATH', allVars.get('PASSPHRASE_PATH'))
                env('USER', allVars.get('USER'))
                env('ACCOUNT', allVars.get('ACCOUNT'))
                env('WITH_RETRY', allVars.get('WITH_RETRY'))
                env('NO_OF_TRIES', allVars.get('NO_OF_TRIES'))
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
            triggers {
                upstream('warehouse-transforms-ci-poll-master', 'SUCCESS')
            }
            publishers common_datadog_build_end(dslFactory, allVars) << common_groovy_postbuild(dslFactory, allVars) << common_publishers(allVars)
            publishers slack_publisher()
            wrappers {
                colorizeOutput('xterm')
            }
            wrappers common_wrappers(allVars)
            steps {
                shell(dslFactory.readFileFromWorkspace('dataeng/resources/datadog_job_start.sh'))
                shell(dslFactory.readFileFromWorkspace('dataeng/resources/secrets-manager-setup.sh'))
                shell(dslFactory.readFileFromWorkspace('dataeng/resources/warehouse-transforms-ci-master-merges.sh'))
            }
        }
    }
}
package analytics

import static org.edx.jenkins.dsl.AnalyticsConstants.common_log_rotator
import static org.edx.jenkins.dsl.AnalyticsConstants.common_publishers
import static org.edx.jenkins.dsl.AnalyticsConstants.common_triggers
import static org.edx.jenkins.dsl.AnalyticsConstants.secure_scm
import static org.edx.jenkins.dsl.AnalyticsConstants.secure_scm_parameters


class DetectNewDBTManualModelsFields {
    public static def job = { dslFactory, allVars ->
        dslFactory.job("detect-new-dbt-manual-models-fields") {
            // If the DISABLED is set to true by the job's extra vars, then disable the job.
            disabled(allVars.get('DISABLED', false))
            description("This job detects new columns in tables in raw schemas that have yet to be manually added to safe schema models.")
            // Set a definite log rotation, if defined.
            logRotator common_log_rotator(allVars)
            // Set the analytics-secure parameters for repo and branch from the common helpers
            parameters secure_scm_parameters(allVars)
            // Add parameters to use analytics-tools and warehouse-transforms
            parameters {
                stringParam('ANALYTICS_TOOLS_URL', allVars.get('ANALYTICS_TOOLS_URL'), 'URL for the analytics tools repo.')
                stringParam('ANALYTICS_TOOLS_BRANCH', allVars.get('ANALYTICS_TOOLS_BRANCH'), 'Branch of analytics tools repo to use.')
                stringParam('WAREHOUSE_TRANSFORMS_URL', allVars.get('WAREHOUSE_TRANSFORMS_URL'), 'URL for the warehouse-transforms repository.')
                stringParam('WAREHOUSE_TRANSFORMS_BRANCH', allVars.get('WAREHOUSE_TRANSFORMS_BRANCH'), 'Branch of warehouse-transforms repository to use.')
                stringParam('DBT_TARGET', allVars.get('DBT_TARGET'), 'DBT target from profiles.yml in analytics-secure.')
                stringParam('DBT_PROFILE', allVars.get('DBT_PROFILE'), 'DBT profile from profiles.yml in analytics-secure.')
                stringParam('DBT_PROJECT_PATH', allVars.get('DBT_PROJECT_PATH'), 'Path in warehouse-transforms to use as the dbt project, relative to "projects" (usually automated/applications or reporting).')
                stringParam('NOTIFY', allVars.get('NOTIFY','$PAGER_NOTIFY'), 'Space separated list of emails to send notifications to.')
            }
            // Set the necessary VAULT kv paths of credentials as environment variables
            environmentVariables {
                env('JIRA_WEBHOOK_VAULT_KV_PATH', allVars.get('JIRA_WEBHOOK_VAULT_KV_PATH'))
                env('JIRA_WEBHOOK_VAULT_KV_VERSION', allVars.get('JIRA_WEBHOOK_VAULT_KV_VERSION'))
                env('AUTOMATION_TASK_USER_VAULT_KV_PATH', allVars.get('AUTOMATION_TASK_USER_VAULT_KV_PATH'))
                env('AUTOMATION_TASK_USER_VAULT_KV_VERSION', allVars.get('AUTOMATION_TASK_USER_VAULT_KV_VERSION'))
            }
            // SCM settings for analytics-secure and analytics-tools
            multiscm secure_scm(allVars) << {
                git {
                    remote {
                        url('$WAREHOUSE_TRANSFORMS_URL')
                        branch('$WAREHOUSE_TRANSFORMS_BRANCH')
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
            }
            wrappers {
                colorizeOutput('xterm')
                timestamps()
                credentialsBinding {
                    usernamePassword('ANALYTICS_VAULT_ROLE_ID', 'ANALYTICS_VAULT_SECRET_ID', 'analytics-vault')
                }
            }
            // Set the trigger using cron
            triggers common_triggers(allVars)
            // Notifications on build failures
            publishers common_publishers(allVars)
            steps {
                shell(dslFactory.readFileFromWorkspace('dataeng/resources/detect-new-dbt-manual-models-fields.sh'))
            }
        }
    }
}

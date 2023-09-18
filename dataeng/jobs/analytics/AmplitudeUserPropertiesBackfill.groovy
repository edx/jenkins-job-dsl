package analytics

import static org.edx.jenkins.dsl.AnalyticsConstants.common_authorization
import static org.edx.jenkins.dsl.AnalyticsConstants.common_log_rotator
import static org.edx.jenkins.dsl.AnalyticsConstants.common_publishers
import static org.edx.jenkins.dsl.AnalyticsConstants.secure_scm_parameters
import static org.edx.jenkins.dsl.AnalyticsConstants.secure_scm


class AmplitudeUserPropertiesBackfill {
    public static def job = { dslFactory, allVars -> 
        dslFactory.job("amplitude-user-properties-backfill") {
            logRotator common_log_rotator(allVars)
            authorization common_authorization(allVars)
            parameters secure_scm_parameters(allVars)
            parameters {
                stringParam('ANALYTICS_TOOLS_URL', allVars.get('ANALYTICS_TOOLS_URL'), 'URL for the analytics tools repo.')
                stringParam('ANALYTICS_TOOLS_BRANCH', allVars.get('ANALYTICS_TOOLS_BRANCH'), 'Branch of analytics tools repo to use.')
                stringParam('NOTIFY', allVars.get('NOTIFY','$PAGER_NOTIFY'), 'Space separated list of emails to send notifications to.')
                stringParam('PYTHON_VENV_VERSION', 'python3.7', 'Python virtual environment version to used.')
                stringParam('AMPLITUDE_DATA_SOURCE_TABLE', '', 'Table name that has data which needs to be updated on Amplitude. It should have format like database.schema.table.')
                stringParam('COLUMNS_TO_UPDATE', '', 'Columns that you want to update. Separate multiple columns with commas.')
                stringParam('RESPONSE_TABLE', '', 'Output table which will store the updated data along with response from API endpoint.')
                stringParam('AMPLITUDE_OPERATION_NAME', '', 'Amplitude user property operation name. e.g: set or setOnce.')
            }
            environmentVariables {
                env('KEY_PATH', allVars.get('KEY_PATH'))
                env('PASSPHRASE_PATH', allVars.get('PASSPHRASE_PATH'))
                env('USER', allVars.get('USER'))
                env('ACCOUNT', allVars.get('ACCOUNT'))
                env('AMPLITUDE_VAULT_KV_PATH', allVars.get('AMPLITUDE_VAULT_KV_PATH'))
                env('AMPLITUDE_VAULT_KV_VERSION', allVars.get('AMPLITUDE_VAULT_KV_VERSION'))
            }
            multiscm secure_scm(allVars) << {
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
                timestamps()
                credentialsBinding {
                    usernamePassword('ANALYTICS_VAULT_ROLE_ID', 'ANALYTICS_VAULT_SECRET_ID', 'analytics-vault');
                }
            }
            publishers common_publishers(allVars)
            steps {
                shell(dslFactory.readFileFromWorkspace('dataeng/resources/amplitude-properties-backfill.sh'))
            }
        }
    }
}

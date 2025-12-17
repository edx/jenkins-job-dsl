package analytics

import static org.edx.jenkins.dsl.AnalyticsConstants.common_authorization
import static org.edx.jenkins.dsl.AnalyticsConstants.common_log_rotator
import static org.edx.jenkins.dsl.AnalyticsConstants.common_publishers
import static org.edx.jenkins.dsl.AnalyticsConstants.secure_scm_parameters
import static org.edx.jenkins.dsl.AnalyticsConstants.secure_scm


class SnowflakeUserRetirementStatusCleanup {
    public static def job = { dslFactory, allVars -> 
        dslFactory.job("snowflake-user-retirement-status-cleanup") {
            description(
                'Remove any soft-deleted user-retirement-statuses from Snowflake. ' +
                'The presence of a soft-deleted row in this table indicates that the ' +
                'user-retirement-status has been successfully archived and removed from ' +
                'the LMS database.'
            )
            logRotator common_log_rotator(allVars)
            authorization common_authorization(allVars)
            parameters secure_scm_parameters(allVars)
            parameters {
                stringParam(
                    'ANALYTICS_TOOLS_URL', allVars.get('ANALYTICS_TOOLS_URL'),
                    'URL for the analytics tools repo.'
                )
                stringParam(
                    'ANALYTICS_TOOLS_BRANCH', allVars.get('ANALYTICS_TOOLS_BRANCH'),
                    'Branch of analytics tools repo to use.'
                )
                stringParam(
                    'NOTIFY', allVars.get('NOTIFY','$PAGER_NOTIFY'),
                    'Space separated list of emails to send notifications to.'
                )
                stringParam('PYTHON_VENV_VERSION', 'python3.11', 'Python virtual environment version to use.')
            }
            environmentVariables {
                // Path to the key file used to authenticate to Snowflake
                env('KEY_PATH', allVars.get('KEY_PATH'))
                // Path to the key-phrase file used to decrypt the key file
                env('PASSPHRASE_PATH', allVars.get('PASSPHRASE_PATH'))
                env('USER', allVars.get('USER'))
                env('ACCOUNT', allVars.get('ACCOUNT'))
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
            triggers {
                // Run on the seventh day of every month. The user-retirement-archiver job
                // runs on the first of every month, so this should allow enough time to
                // address any issues with the archival process.
                cron('H H 7 * *')
            }
            wrappers {
                timestamps()
            }
            publishers common_publishers(allVars)
            steps {
                shell(dslFactory.readFileFromWorkspace('dataeng/resources/snowflake-user-retirement-status-cleanup.sh'))
            }
        }
    }
}

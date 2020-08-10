package analytics

import static org.edx.jenkins.dsl.AnalyticsConstants.common_log_rotator
import static org.edx.jenkins.dsl.AnalyticsConstants.common_publishers
import static org.edx.jenkins.dsl.AnalyticsConstants.common_triggers
import static org.edx.jenkins.dsl.AnalyticsConstants.secure_scm
import static org.edx.jenkins.dsl.AnalyticsConstants.secure_scm_parameters


class SnowflakeValidateStitch {
    public static def job = { dslFactory, allVars ->
        dslFactory.job('snowflake-validate-stitch') {

            description(
                'Validate application database tables loaded by Stitch by comparing them against the same ' +
                'tables loaded by Sqoop.  This compares only tables that exist in both sets, and only the last ' +
                '10 days of changed rows.'
            )

            parameters secure_scm_parameters(allVars)
            parameters {
                stringParam('ANALYTICS_TOOLS_URL', allVars.get('ANALYTICS_TOOLS_URL'), 'URL for the analytics tools repo.')
                stringParam('ANALYTICS_TOOLS_BRANCH', allVars.get('ANALYTICS_TOOLS_BRANCH'), 'Branch of analytics tools repo to use.')
                stringParam('APP_NAME', '', 'Application name of tables to validate.')
                stringParam('SQOOP_START_TIME', '', 'Application name of tables to validate.')
                stringParam('SNOWFLAKE_USER', 'SNOWFLAKE_TASK_AUTOMATION_USER')
                stringParam('SNOWFLAKE_ACCOUNT', 'edx.us-east-1')
                stringParam('SNOWFLAKE_KEY_PATH', 'snowflake/rsa_key_snowflake_task_automation_user.p8', 'Path to the encrypted private key file that corresponds to the SNOWFLAKE_USER, relative to the root of analytics-secure.')
                stringParam('SNOWFLAKE_PASSPHRASE_PATH', 'snowflake/rsa_key_passphrase_snowflake_task_automation_user', 'Path to the private key decryption passphrase file that corresponds to the SNOWFLAKE_USER, relative to the root of analytics-secure.')
                stringParam('NOTIFY', allVars.get('NOTIFY','$PAGER_NOTIFY'), 'Space separated list of emails to send notifications to.')
            }
            logRotator common_log_rotator(allVars)
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
                buildName('#${BUILD_NUMBER} (${ENV,var="APP_NAME"})')
            }
            publishers common_publishers(allVars)
            steps {
                virtualenv {
                    pythonName('PYTHON_3.7')
                    nature("shell")
                    systemSitePackages(false)
                    command(
                        dslFactory.readFileFromWorkspace('dataeng/resources/snowflake-validate-stitch.sh')
                    )
                }
            }
        }
    }
}

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
            }
            environmentVariables {
                env('KEY_PATH', allVars.get('KEY_PATH'))
                env('PASSPHRASE_PATH', allVars.get('PASSPHRASE_PATH'))
                env('USER', allVars.get('USER'))
                env('ACCOUNT', allVars.get('ACCOUNT'))
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

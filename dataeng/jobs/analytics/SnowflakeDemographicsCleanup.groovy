package analytics

import static org.edx.jenkins.dsl.AnalyticsConstants.common_authorization
import static org.edx.jenkins.dsl.AnalyticsConstants.common_log_rotator
import static org.edx.jenkins.dsl.AnalyticsConstants.common_publishers
import static org.edx.jenkins.dsl.AnalyticsConstants.secure_scm_parameters
import static org.edx.jenkins.dsl.AnalyticsConstants.secure_scm


class SnowflakeDemographicsCleanup {
    public static def job = { dslFactory, allVars -> 
        dslFactory.job("snowflake-demographics-cleanup") {
            logRotator common_log_rotator(allVars)
            authorization common_authorization(allVars)
            parameters secure_scm_parameters(allVars)
            parameters {
                stringParam('ANALYTICS_TOOLS_URL', allVars.get('ANALYTICS_TOOLS_URL'), 'URL for the analytics tools repo.')
                stringParam('ANALYTICS_TOOLS_BRANCH', allVars.get('ANALYTICS_TOOLS_BRANCH'), 'Branch of analytics tools repo to use.')
                stringParam('NOTIFY', allVars.get('NOTIFY','$PAGER_NOTIFY'), 'Space separated list of emails to send notifications to.')
            }
            environmentVariables {
                env('KEY_PATH', allVars.get('KEY_PATH'))
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
                // Daily between 06:00 - 07:00
                cron('H 6 * * *')
            }
            wrappers {
                timestamps()
            }
            publishers common_publishers(allVars)
            steps {
                virtualenv {
                    pythonName('PYTHON_3.7')
                    nature("shell")
                    systemSitePackages(false)
                    command(
                        dslFactory.readFileFromWorkspace("dataeng/resources/snowflake-demographics-cleanup.sh")
                    )
                }
            }
        }
    }
}

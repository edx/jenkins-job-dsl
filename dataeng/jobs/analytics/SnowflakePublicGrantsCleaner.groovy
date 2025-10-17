package analytics

import static org.edx.jenkins.dsl.AnalyticsConstants.common_multiscm
import static org.edx.jenkins.dsl.AnalyticsConstants.common_parameters
import static org.edx.jenkins.dsl.AnalyticsConstants.common_log_rotator
import static org.edx.jenkins.dsl.AnalyticsConstants.common_wrappers
import static org.edx.jenkins.dsl.AnalyticsConstants.common_publishers
import static org.edx.jenkins.dsl.AnalyticsConstants.common_triggers



class SnowflakePublicGrantsCleaner {
    public static def job = { dslFactory, allVars ->
        dslFactory.job("snowflake-public-grants-cleaner") {
            disabled()
            logRotator common_log_rotator(allVars)
            parameters {
                stringParam('ANALYTICS_TOOLS_URL', allVars.get('ANALYTICS_TOOLS_URL'), 'URL for the analytics tools repo.')
                stringParam('ANALYTICS_TOOLS_BRANCH', allVars.get('ANALYTICS_TOOLS_BRANCH'), 'Branch of analtyics tools repo to use.')
                stringParam('NOTIFY', allVars.get('NOTIFY','$PAGER_NOTIFY'), 'Space separated list of emails to send notifications to.')
                stringParam('PYTHON_VENV_VERSION', 'python3.7', 'Python virtual environment version to used.')
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
            triggers common_triggers(allVars)
            wrappers {
                timestamps()
            }
            publishers common_publishers(allVars)
            steps {
                shell(dslFactory.readFileFromWorkspace('dataeng/resources/snowflake-public-grants-cleaner.sh'))
            }
        }
    }
}

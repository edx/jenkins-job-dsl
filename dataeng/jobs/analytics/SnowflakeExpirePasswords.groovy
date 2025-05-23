package analytics

import static org.edx.jenkins.dsl.AnalyticsConstants.common_log_rotator
import static org.edx.jenkins.dsl.AnalyticsConstants.common_publishers
import static org.edx.jenkins.dsl.AnalyticsConstants.common_triggers


class SnowflakeExpirePasswords {

    public static def job = { dslFactory, allVars ->

        Map SnowflakeExpirePasswordsQuarterlyConfig = [
            NAME: 'expire-snowflake-passwords-quarterly',
            MANUAL: false,
            SCRIPT_TO_RUN: 'dataeng/resources/snowflake-expire-passwords.sh'
        ]
        Map SnowflakeExpirePasswordsManuallyConfig = [
            NAME: 'expire-snowflake-passwords-manually',
            MANUAL: true,
            SCRIPT_TO_RUN: 'dataeng/resources/snowflake-expire-individual-password.sh'
        ]
        List jobConfigs = [
            SnowflakeExpirePasswordsQuarterlyConfig,
            SnowflakeExpirePasswordsManuallyConfig
        ]

        jobConfigs.each { jobConfig ->

            dslFactory.job(jobConfig['NAME']) {

                logRotator common_log_rotator(allVars)
                parameters {
                    stringParam('ANALYTICS_TOOLS_URL', allVars.get('ANALYTICS_TOOLS_URL'), 'URL for the analytics tools repo.')
                    stringParam('ANALYTICS_TOOLS_BRANCH', allVars.get('ANALYTICS_TOOLS_BRANCH'), 'Branch of analytics tools repo to use.')
                    stringParam('NOTIFY', allVars.get('NOTIFY','$PAGER_NOTIFY'), 'Space separated list of emails to send notifications to.')
                    if (jobConfig['MANUAL']) {
                        stringParam('USER_TO_EXPIRE', '', 'Snowflake user to mark for password expiration.')
                    }
                }
                environmentVariables {
                    env('USER', allVars.get('USER'))
                    env('ACCOUNT', allVars.get('ACCOUNT'))
                }
                logRotator common_log_rotator(allVars)
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
                if (!jobConfig['MANUAL']) {
                    triggers common_triggers(allVars)
                }
                wrappers {
                    timestamps()
                }
                publishers common_publishers(allVars)
                steps {
                    shell(dslFactory.readFileFromWorkspace(jobConfig['SCRIPT_TO_RUN']))
                }
            }
        }
    }
}

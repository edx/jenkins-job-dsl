package analytics

import static org.edx.jenkins.dsl.AnalyticsConstants.common_log_rotator
import static org.edx.jenkins.dsl.AnalyticsConstants.common_triggers
import static org.edx.jenkins.dsl.AnalyticsConstants.common_publishers
import static org.edx.jenkins.dsl.AnalyticsConstants.secure_scm
import static org.edx.jenkins.dsl.AnalyticsConstants.secure_scm_parameters


class SnowflakeQueueDepth {

    public static def job = { dslFactory, allVars ->

        Map SnowflakeQueueDepthConfig = [
            NAME: "collect-queue-depth-hourly",
            MANUAL: false,
            SCRIPT_TO_RUN: "dataeng/resources/snowflake-collect-queue-depth.sh"
        ]
        List jobConfigs = [
            SnowflakeQueueDepthConfig
        ]

        jobConfigs.each { jobConfig ->

            dslFactory.job(jobConfig['NAME']) {

                logRotator common_log_rotator(allVars)
                parameters secure_scm_parameters(allVars)
                parameters {
                    stringParam('ANALYTICS_TOOLS_URL', allVars.get('ANALYTICS_TOOLS_URL'), 'URL for the analytics tools repo.')
                    stringParam('ANALYTICS_TOOLS_BRANCH', allVars.get('ANALYTICS_TOOLS_BRANCH'), 'Branch of analytics tools repo to use.')
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
                if (!jobConfig['MANUAL']) {
                    triggers common_triggers(allVars)
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
                            dslFactory.readFileFromWorkspace(jobConfig['SCRIPT_TO_RUN'])
                        )
                    }
                }
            }
        }
    }
}
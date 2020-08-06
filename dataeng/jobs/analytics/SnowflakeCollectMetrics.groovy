package analytics

import static org.edx.jenkins.dsl.AnalyticsConstants.common_log_rotator
import static org.edx.jenkins.dsl.AnalyticsConstants.common_publishers
import static org.edx.jenkins.dsl.AnalyticsConstants.secure_scm
import static org.edx.jenkins.dsl.AnalyticsConstants.secure_scm_parameters


class SnowflakeCollectMetrics {

    public static def job = { dslFactory, allVars ->

        Map SnowflakeWarehouseCreditConfig = [
            NAME: 'snowflake-collect-credit-metrics',
            CRON: '0 * * * *'
        ]
        Map SnowflakeQueueDepthConfig = [
            NAME: 'snowflake-collect-queue-depth',
            CRON: '*/5 * * * *'
        ]
        List jobConfigs = [
            SnowflakeWarehouseCreditConfig,
            SnowflakeQueueDepthConfig
        ]

        jobConfigs.each { jobConfig ->

            dslFactory.job(jobConfig['NAME']){

                logRotator common_log_rotator(allVars)
                parameters secure_scm_parameters(allVars)
                parameters {
                    stringParam('ANALYTICS_TOOLS_URL', allVars.get('ANALYTICS_TOOLS_URL'), 'URL for the analytics tools repo.')
                    stringParam('ANALYTICS_TOOLS_BRANCH', allVars.get('ANALYTICS_TOOLS_BRANCH'), , 'Branch of analytics tools repo to use.')
                    stringParam('NOTIFY', '$PAGER_NOTIFY', 'Space separated list of emails to send notifications to.')
                }
                environmentVariables {
                    env('SNOWFLAKE_USER', 'SNOWFLAKE_TASK_AUTOMATION_USER')
                    env('SNOWFLAKE_ACCOUNT', 'edx.us-east-1')
                    env('METRIC_NAME', jobConfig['NAME'])
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
                    cron(jobConfig['CRON'])
                }
                wrappers {
                    timestamps()
                }
                publishers common_publishers(allVars)
                steps {
                    virtualenv {
                        pythonName('PYTHON_3.7')
                        nature('shell')
                        systemSitePackages(false)
                        command(
                            dslFactory.readFileFromWorkspace('dataeng/resources/snowflake-collect-metrics.sh')
                        )
                    }
                }
            }
        }
    }
}

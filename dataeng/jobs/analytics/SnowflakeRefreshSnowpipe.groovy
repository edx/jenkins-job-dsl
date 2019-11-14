package analytics

import static org.edx.jenkins.dsl.AnalyticsConstants.common_multiscm
import static org.edx.jenkins.dsl.AnalyticsConstants.common_parameters
import static org.edx.jenkins.dsl.AnalyticsConstants.common_log_rotator
import static org.edx.jenkins.dsl.AnalyticsConstants.common_wrappers
import static org.edx.jenkins.dsl.AnalyticsConstants.common_publishers
import static org.edx.jenkins.dsl.AnalyticsConstants.common_triggers
import static org.edx.jenkins.dsl.AnalyticsConstants.secure_scm_parameters
import static org.edx.jenkins.dsl.AnalyticsConstants.secure_scm


// If you want to configure another table to a Snowpipe, create a config map
// here and add it to the jobConfigs list.
Map eventSnowpipeConfig = [
    NAME: 'refresh-snowpipe-event-loader',
    PIPE_NAME: 'snowpipe_event_loader',
    TABLE_NAME: 'json_event_records',
    DELAY: 120,
    LIMIT: 7
]
List jobConfigs = [
    eventSnowpipeConfig
]

class SnowflakeRefreshSnowpipe {
    public static def job = { dslFactory, allVars ->

        jobConfigs.each { jobConfig ->

            dslFactory.job(jobConfig['NAME']) {

                // This job should be disabled until the rest of the work in DE-1779 is complete!
                disabled(true)

                logRotator common_log_rotator(allVars)
                parameters secure_scm_parameters(allVars)
                parameters {
                    stringParam('ANALYTICS_TOOLS_URL', allVars.get('ANALYTICS_TOOLS_URL'), 'URL for the analytics tools repo.')
                    stringParam('ANALYTICS_TOOLS_BRANCH', allVars.get('ANALYTICS_TOOLS_BRANCH'), 'Branch of analtyics tools repo to use.')
                    stringParam('PIPE_NAME', jobConfig['PIPE_NAME'], 'Name of Snowflake snowpipe to refresh')
                    stringParam('TABLE_NAME', jobConfig['TABLE_NAME'], 'Name of Snowflake table that receives snowpipe data')
                    stringParam('DELAY', jobConfig['DELAY'], 'Time (in seconds) to wait between issuing commands')
                    stringParam('LIMIT', jobConfig['LIMIT'], 'Minimum number of expected data files in the copy history for the snowpipe')
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
                triggers common_triggers(allVars)
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
                            dslFactory.readFileFromWorkspace("dataeng/resources/snowflake-refresh-snowpipe.sh")
                        )
                    }
                }
            }
        }
    }
}

package analytics

import static org.edx.jenkins.dsl.AnalyticsConstants.common_multiscm
import static org.edx.jenkins.dsl.AnalyticsConstants.common_parameters
import static org.edx.jenkins.dsl.AnalyticsConstants.common_log_rotator
import static org.edx.jenkins.dsl.AnalyticsConstants.common_wrappers
import static org.edx.jenkins.dsl.AnalyticsConstants.common_publishers
import static org.edx.jenkins.dsl.AnalyticsConstants.common_triggers



class SnowflakeRefreshSnowpipe {

    public static def job = { dslFactory, allVars ->

        // If you want to configure another table to a Snowpipe, create a config map
        // here and add it to the jobConfigs list.
        Map trackingProdLogEventSnowpipeConfig = [
            NAME: 'refresh-snowpipe-event-loader',
            PIPE_NAME: 'prod.events.prod_edx_snowpipe_event_loader',
            TABLE_NAME: 'prod.events.tracking_log_events_raw',
            SCHEMA: 'prod.events',
            DELAY: '120',
            LIMIT: '7'
        ]
        Map segmentProdLogEventSnowpipeConfig = [
            NAME: 'refresh-snowpipe-segment-prod-event-loader',
            PIPE_NAME: 'prod.segment_events_raw.prod_segment_snowpipe_event_s3_pipe',
            TABLE_NAME: 'prod.segment_events_raw.segment_log_events_raw',
            SCHEMA: 'prod.segment_events_raw',
            DELAY: '120',
            LIMIT: '7'
        ]
        List jobConfigs = [
            trackingProdLogEventSnowpipeConfig,
            segmentProdLogEventSnowpipeConfig
        ]

        jobConfigs.each { jobConfig ->

            dslFactory.job(jobConfig['NAME']) {

                logRotator common_log_rotator(allVars)
                parameters {
                    stringParam('ANALYTICS_TOOLS_URL', allVars.get('ANALYTICS_TOOLS_URL'), 'URL for the analytics tools repo.')
                    stringParam('ANALYTICS_TOOLS_BRANCH', allVars.get('ANALYTICS_TOOLS_BRANCH'), 'Branch of analtyics tools repo to use.')
                    stringParam('PIPE_NAME', jobConfig['PIPE_NAME'], 'Name of Snowflake snowpipe to refresh')
                    stringParam('TABLE_NAME', jobConfig['TABLE_NAME'], 'Name of Snowflake table that receives snowpipe data')
                    stringParam('SCHEMA', jobConfig['SCHEMA'], 'Schema which contains the Snowpipe and loaded table in format <db>.<schema>')
                    stringParam('DELAY', jobConfig['DELAY'], 'Time (in seconds) to wait between issuing commands')
                    stringParam('LIMIT', jobConfig['LIMIT'], 'Minimum number of expected data files in the copy history for the snowpipe')
                    stringParam('NOTIFY', allVars.get('NOTIFY','$PAGER_NOTIFY'), 'Space separated list of emails to send notifications to.')
                    stringParam('PYTHON_VENV_VERSION', 'python3.7', 'Python virtual environment version to used.')
                }
                environmentVariables {
                    env('KEY_PATH', allVars.get('KEY_PATH'))
                    env('PASSPHRASE_PATH', allVars.get('PASSPHRASE_PATH'))
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
                triggers common_triggers(allVars)
                wrappers {
                    timestamps()
                }
                publishers common_publishers(allVars)
                steps {
                    shell(dslFactory.readFileFromWorkspace('dataeng/resources/snowflake-refresh-snowpipe.sh'))
                }
            }
        }
    }
}

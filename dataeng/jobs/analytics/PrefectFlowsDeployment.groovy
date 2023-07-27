package analytics
import static org.edx.jenkins.dsl.AnalyticsConstants.secure_scm
import static org.edx.jenkins.dsl.AnalyticsConstants.common_log_rotator
import static org.edx.jenkins.dsl.AnalyticsConstants.common_wrappers
import static org.edx.jenkins.dsl.AnalyticsConstants.common_publishers
import static org.edx.jenkins.dsl.AnalyticsConstants.common_triggers
import static org.edx.jenkins.dsl.AnalyticsConstants.secure_scm_parameters
import static org.edx.jenkins.dsl.AnalyticsConstants.common_authorization
import static org.edx.jenkins.dsl.AnalyticsConstants.slack_publisher


class PrefectFlowsDeployment{
    public static def job = { dslFactory, allVars ->
        dslFactory.job("prefect-flows-deployment-poll"){
            description(
                'This job polls prefect-flows repository code changes to trigger deployment ' +
                'It detects file changes for latest merge commit, creates a downstream.properties ' +
                ' file to pass comma delimeted list of flows in FLOWS_TO_DEPLOY parameter to downstream job'
            )
            authorization common_authorization(allVars)
            logRotator {
                numToKeep(180)
            }
            parameters secure_scm_parameters(allVars)
            parameters {
                stringParam('PREFECT_FLOWS_URL', allVars.get('PREFECT_FLOWS_URL'), 'URL for the prefect-flows repository.')
                stringParam('PREFECT_FLOWS_BRANCH', allVars.get('PREFECT_FLOWS_BRANCH'), 'Branch of prefect-flows repository to use.')
                stringParam('NOTIFY', allVars.get('NOTIFY','$PAGER_NOTIFY'), 'Space separated list of emails to send notifications to.')
            }
            scm {
                // As it is a poll job it should only use git scm for prefect-flows code repository
                // Do not add anyother git scm in this poll job
                git {
                    remote {
                        url('$PREFECT_FLOWS_URL')
                        branch('$PREFECT_FLOWS_BRANCH')
                    }
                    extensions {
                        relativeTargetDirectory('prefect-flows')
                        pruneBranches()
                        cleanAfterCheckout()
                    }
                }
            }
            triggers {
                pollSCM {
                    scmpoll_spec(allVars.get('JOB_FREQUENCY'))
                }
            }
            publishers common_publishers(allVars)
            wrappers common_wrappers(allVars)
            wrappers {
                colorizeOutput('xterm')
                timestamps()
                credentialsBinding {
                    usernamePassword('GITHUB_USER', 'GITHUB_TOKEN', 'GITHUB_USER_PASS_COMBO');
                }
            }
            steps {
                shell(dslFactory.readFileFromWorkspace('dataeng/resources/prefect-flows-deployment-identify.sh'))
            }
            publishers {
                downstreamParameterized {
                    trigger('prefect-flows-deployment-intermediate') {
                        condition('SUCCESS')
                        parameters {
                            // The contents of this file are generated as part of the shell script in build step.
                            propertiesFile('${WORKSPACE}/downstream.properties')
                        }
                    }
                }
            }
        }
        dslFactory.job("prefect-flows-deployment-intermediate"){
            description(
                'It receives comma delimated list of FLOWS_TO_DEPLOY and use this parameter to trigger downstream ' +
                'Environment variables created inside shell script can not be referred back in same DSL again ' +
                'This job works as intermediate to get FLOWS_TO_DEPLOY and use it to call relevant downstream deployment jobs'
            )
            authorization common_authorization(allVars)
            logRotator {
                numToKeep(180)
            }
            parameters secure_scm_parameters(allVars)
            parameters {
                stringParam('FLOWS_TO_DEPLOY', allVars.get('FLOWS_TO_DEPLOY'), 'Comma separated list of Flows that upstream job has identified and needs to redeploy')
            }
            wrappers common_wrappers(allVars)
            publishers {
                downstreamParameterized {
                    trigger('$FLOWS_TO_DEPLOY') {
                        condition('SUCCESS')
                        parameters {
                            // Added parameter to pass name flows to be deployed
                            predefinedProp('FLOWS_TO_DEPLOY', '$FLOWS_TO_DEPLOY')
                        }
                    }
                }
            }
        }
        List prefect_flows = [
            'load_affiliate_window_transactions_to_snowflake',
            'load_course_structure_to_snowflake',
            'load_cybersource_reports_to_snowflake',
            'load_edx_sitemap_to_snowflake',
            'load_enterprise_tables_from_s3_to_aurora',
            'load_geoip_to_snowflake',
            'load_google_analytics_data_to_snowflake',
            'load_google_sheets_to_snowflake',
            'load_insights_tables_from_snowflake_to_aurora',
            'load_paypal_cases_to_snowflake',
            'load_paypal_settlement_report_to_snowflake',
            'load_paypal_transaction_detail_report_to_snowflake',
            'send_hubspot_leads_to_braze',
            'send_hubspot_unsubscribes_to_braze',
            'load_segment_config_to_snowflake',
            'send_transactions_to_vertex',
            'send_refunds_to_vertex',
            'send_transactions_refunds_to_vertex',
        ]
        prefect_flows.each { prefect_flow ->
            dslFactory.job("prefect-flows-deployment-$prefect_flow"){
                description(
                    'Changes detected in upstream job gets trigger this prefect-flows-deployment job ' +
                    'It installs requirements, autheticates prefect and deploy latest code changes'
                )
                // if name of this job is ever changed. Make to sure to update deletion of job name prefix in prefect-deployment-identify.sh
                authorization common_authorization(allVars)
                logRotator {
                    numToKeep(180)
                }
                parameters secure_scm_parameters(allVars)
                parameters {
                    stringParam('PREFECT_FLOWS_URL', allVars.get('PREFECT_FLOWS_URL'), 'URL for the prefect-flows repository.')
                    stringParam('PREFECT_FLOWS_BRANCH', allVars.get('PREFECT_FLOWS_BRANCH'), 'Branch of prefect-flows repository to use.')
                    stringParam('ECR_LOGIN', allVars.get('ECR_LOGIN'), 'ECR repository URI used to login to ECR')
                    stringParam('NOTIFY', allVars.get('NOTIFY','$PAGER_NOTIFY'), 'Space separated list of emails to send notifications to.')
                }
                environmentVariables {
                    env('PREFECT_VAULT_KV_PATH', allVars.get('PREFECT_VAULT_KV_PATH'))
                    env('PREFECT_VAULT_KV_VERSION', allVars.get('PREFECT_VAULT_KV_VERSION'))
                }
                multiscm secure_scm(allVars) << {
                    git {
                        remote {
                            url('$PREFECT_FLOWS_URL')
                            branch('$PREFECT_FLOWS_BRANCH')
                        }
                        extensions {
                            relativeTargetDirectory('prefect-flows')
                            pruneBranches()
                            cleanAfterCheckout()
                        }
                    }
                }
                publishers common_publishers(allVars)
                wrappers {
                    colorizeOutput('xterm')
                    timestamps()
                    credentialsBinding {
                        usernamePassword('ANALYTICS_VAULT_ROLE_ID', 'ANALYTICS_VAULT_SECRET_ID', 'analytics-vault');
                        usernamePassword('GITHUB_USER', 'GITHUB_TOKEN', 'GITHUB_USER_PASS_COMBO');
                    }
                }
                steps {
                    shell(dslFactory.readFileFromWorkspace('dataeng/resources/prefect-flows-deployment.sh'))
                }
            }
        }
    }
}

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
                ' file to pass comma delimeted list of flows in FLOW_NAME parameter to downstream job'
            )
            authorization common_authorization(allVars)
            logRotator common_log_rotator(allVars)
            parameters secure_scm_parameters(allVars)
            parameters {
                stringParam('PREFECT_FLOWS_URL', allVars.get('PREFECT_FLOWS_URL'), 'URL for the prefect-flows repository.')
                stringParam('PREFECT_FLOWS_BRANCH', allVars.get('PREFECT_FLOWS_BRANCH'), 'Branch of prefect-flows repository to use.')
            }
            scm {
                // As it is a poll job it should only use git scm for prefect-flows code repository
                // Do not add anyother git scm in this poll job
                git {
                    remote {
                        url('$PREFECT_FLOWS_URL')
                        branch('$PREFECT_FLOWS_BRANCH')
                        //credentials('1')
                    }
                    extensions {
                        relativeTargetDirectory('prefect-flows')
                        pruneBranches()
                        cleanAfterCheckout()
                    }
                }

            }
            triggers common_triggers(allVars, allVars)
            // triggers {
            //     scm('H/2 * * * *') // change it to 10 minutes after testing
            // }
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
            environmentVariables {
                env('ONE', 'testing')
            }
            publishers {
                downstreamParameterized {
                    trigger('$ONE') {
                        condition('SUCCESS')
                        parameters {
                            // The contents of this file are generated as part of the script in the build step.
                            propertiesFile('${WORKSPACE}/downstream.properties')
                        }
                    }
                }
            }
        }
        dslFactory.job("prefect-flows-deployment-intermediate"){
            description(
                'It receives comma delimated list of FLOW_NAME and use this parameter to trigger downstream ' +
                'Environment variables created inside shell script can not be refered back in same DSL again ' +
                'This jobs works as intermediate to get FLOW_NAME and use it to call respective downstream deployment jobs'
            )
            authorization common_authorization(allVars)
            logRotator common_log_rotator(allVars)
            parameters secure_scm_parameters(allVars)
            parameters {
                stringParam('PREFECT_FLOWS_URL', allVars.get('PREFECT_FLOWS_URL'), 'URL for the prefect-flows repository.')
                stringParam('PREFECT_FLOWS_BRANCH', allVars.get('PREFECT_FLOWS_BRANCH'), 'Branch of prefect-flows repository to use.')
                stringParam('FLOW_NAME', allVars.get('FLOW_NAME'), 'Comma separated list of Flows that upstream job has identified and needs to redeploy')
            }
            scm {
                git {
                    remote {
                        url('$PREFECT_FLOWS_URL')
                        branch('$PREFECT_FLOWS_BRANCH')
                        //credentials('1')
                    }
                    extensions {
                        relativeTargetDirectory('prefect-flows')
                        pruneBranches()
                        cleanAfterCheckout()
                    }
                }

            }
            wrappers {
                colorizeOutput('xterm')
                timestamps()
                credentialsBinding {
                    usernamePassword('GITHUB_USER', 'GITHUB_TOKEN', 'GITHUB_USER_PASS_COMBO');
                }
            }
            wrappers common_wrappers(allVars)
            publishers {
                downstreamParameterized {
                    trigger('$FLOW_NAME') {
                        condition('SUCCESS')
                        parameters {
                            // Added parameter to pass name flows to be deployed
                            predefinedProp('FLOW_NAME', '$FLOW_NAME')
                        }
                    }
                }
            }
        }
        allVars.get('PREFECT_FLOWS').each { prefect_flows ->
            dslFactory.job("prefect-flows-deployment-$prefect_flows"){
                description(
                    'Changes detected in upstream job gets trigger this prefect-flows-deployment job ' +
                    'It installs requirements, autheticates prefect and deploy latest code changes'
                )
                // if name of this job is ever changed. Make to sure to update deletion of job name prefix in prefect-deployment-identify.sh
                authorization common_authorization(allVars)
                logRotator common_log_rotator(allVars)
                parameters secure_scm_parameters(allVars)
                parameters {
                    stringParam('PREFECT_FLOWS_URL', allVars.get('PREFECT_FLOWS_URL'), 'URL for the prefect-flows repository.')
                    stringParam('PREFECT_FLOWS_BRANCH', allVars.get('PREFECT_FLOWS_BRANCH'), 'Branch of prefect-flows repository to use.')
                    stringParam('EDX_PREFECTUTILS_URL', allVars.get('EDX_PREFECTUTILS_URL'), 'URL for the edx-prefectutils repository.')
                    stringParam('EDX_PREFECTUTILS_BRANCH', allVars.get('EDX_PREFECTUTILS_BRANCH'), 'Branch of edx-prefectutils repository to use.')
                    stringParam('FLOW_NAME', allVars.get('FLOW_NAME'), 'Comma separated list of Flows that upstream job has identified and needs to redeploy')
                    stringParam('ECR_LOGIN', allVars.get('ECR_LOGIN'), 'ECR repository URI used to login to ECR')
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
                            //credentials('1')
                        }
                        extensions {
                            relativeTargetDirectory('prefect-flows')
                            pruneBranches()
                            cleanAfterCheckout()
                        }
                    }
                    git {
                        remote {
                            url('$EDX_PREFECTUTILS_URL')
                            branch('$EDX_PREFECTUTILS_BRANCH')
                            //credentials('1')
                        }
                        extensions {
                            relativeTargetDirectory('edx-prefectutils')
                            pruneBranches()
                            cleanAfterCheckout()
                        }
                    }
                }
                // Enable build failure notifer after testing for few days in Production
                // publishers common_publishers(allVars)
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
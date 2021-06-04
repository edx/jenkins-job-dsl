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
            authorization common_authorization(allVars)
            logRotator common_log_rotator(allVars)
            parameters secure_scm_parameters(allVars)
            parameters {
                stringParam('PREFECT_FLOWS_URL', allVars.get('PREFECT_FLOWS_URL'), 'URL for the prefect-flows repository.')
                stringParam('PREFECT_FLOWS_BRANCH', allVars.get('PREFECT_FLOWS_BRANCH'), 'Branch of prefect-flows repository to use.')
                stringParam('JENKINS_JOB_DSL_URL', allVars.get('JENKINS_JOB_DSL_URL'), 'URL for the jenkins-job-dsl repo.')
                stringParam('JENKINS_JOB_DSL_BRANCH', allVars.get('JENKINS_JOB_DSL_BRANCH'), 'Branch of jenkins-job-dsl repo to use.')
            }
            scm {
                git {
                    remote {
                        url('$PREFECT_FLOWS_URL')
                        branch('$PREFECT_FLOWS_BRANCH')
                        credentials('1')
                    }
                    extensions {
                        relativeTargetDirectory('prefect-flows')
                        pruneBranches()
                        cleanAfterCheckout()
                    }
                }

            }
            triggers {
                scm('H/2 * * * *') // change it to 10 minutes after testing
            }
            wrappers {
                colorizeOutput('xterm')
            }
            wrappers common_wrappers(allVars)
            steps {
                virtualenv {
                    pythonName('PYTHON_3.7')
                    nature("shell")
                    systemSitePackages(false)
                    command(
                        dslFactory.readFileFromWorkspace("dataeng/resources/prefect-flows-deployment-identify.sh")
                    )
                }
            }
            publishers {
                downstreamParameterized {
                    trigger('prefect-flows-deployment-intermediate') {
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
            authorization common_authorization(allVars)
            logRotator common_log_rotator(allVars)
            parameters secure_scm_parameters(allVars)
            parameters {
                stringParam('PREFECT_FLOWS_URL', allVars.get('PREFECT_FLOWS_URL'), 'URL for the prefect-flows repository.')
                stringParam('PREFECT_FLOWS_BRANCH', allVars.get('PREFECT_FLOWS_BRANCH'), 'Branch of prefect-flows repository to use.')
                stringParam('JENKINS_JOB_DSL_URL', allVars.get('JENKINS_JOB_DSL_URL'), 'URL for the jenkins-job-dsl repo.')
                stringParam('JENKINS_JOB_DSL_BRANCH', allVars.get('JENKINS_JOB_DSL_BRANCH'), 'Branch of jenkins-job-dsl repo to use.')
                stringParam('FLOW_NAME', allVars.get('FLOW_NAME'), 'Name of job to run.')
            }
            scm {
                git {
                    remote {
                        url('$PREFECT_FLOWS_URL')
                        branch('$PREFECT_FLOWS_BRANCH')
                        credentials('1')
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
                // if name of this job is ever changed. Make to sure to update deletion of job name prefix in prefect-deployment-identify.sh
                authorization common_authorization(allVars)
                logRotator common_log_rotator(allVars)
                parameters secure_scm_parameters(allVars)
                parameters {
                    stringParam('PREFECT_FLOWS_URL', allVars.get('PREFECT_FLOWS_URL'), 'URL for the prefect-flows repository.')
                    stringParam('PREFECT_FLOWS_BRANCH', allVars.get('PREFECT_FLOWS_BRANCH'), 'Branch of prefect-flows repository to use.')
                    stringParam('EDX_PREFECTUTILS_URL', allVars.get('EDX_PREFECTUTILS_URL'), 'URL for the prefect-flows repository.')
                    stringParam('EDX_PREFECTUTILS_BRANCH', allVars.get('EDX_PREFECTUTILS_BRANCH'), 'Branch of prefect-flows repository to use.')
                    stringParam('FLOW_NAME', allVars.get('FLOW_NAME'), 'Flow name')
                    //stringParam('PREFECT_API_TOKEN', allVars.get('PREFECT_API_TOKEN'), 'Space separated list of emails to send notifications to.')
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
                            credentials('1')
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
                            credentials('1')
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
                    credentialsBinding {
                        usernamePassword('ANALYTICS_VAULT_ROLE_ID', 'ANALYTICS_VAULT_SECRET_ID', 'analytics-vault');
                    }
                }
                steps {
                    shell(dslFactory.readFileFromWorkspace('dataeng/resources/prefect-flows-deployment.sh'))
                }
            }
        }
    }
}
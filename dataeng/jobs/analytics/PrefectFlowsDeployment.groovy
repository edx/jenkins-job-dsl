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
                //stringParam('NOTIFY', allVars.get('NOTIFY','$PAGER_NOTIFY'), 'Space separated list of emails to send notifications to.')
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
                scm('H/1 * * * *') // change it to 10 minutes
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
                    trigger('prefect-flows-deployment-'+ flow_name) {
                        condition('SUCCESS')
                        parameters {
                            // The contents of this file are generated as part of the script in the build step.
                            propertiesFile('${WORKSPACE}/downstream.properties')
                        }
                    }
                }
            }
        }

        dslFactory.job("prefect-flows-deployment-sample_flow"){
            authorization common_authorization(allVars)
            logRotator common_log_rotator(allVars)
            parameters secure_scm_parameters(allVars)
            parameters {
                stringParam('PREFECT_FLOWS_URL', allVars.get('PREFECT_FLOWS_URL'), 'URL for the prefect-flows repository.')
                stringParam('PREFECT_FLOWS_BRANCH', allVars.get('PREFECT_FLOWS_BRANCH'), 'Branch of prefect-flows repository to use.')
                stringParam('DB_NAME', allVars.get('DB_NAME'), 'Database name used to create output schema of dbt run/tests')
                //stringParam('NOTIFY', allVars.get('NOTIFY'), 'Space separated list of emails to send notifications to.')
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
            }
            // triggers {
            //     upstream('warehouse-transforms-ci-poll-master', 'SUCCESS')
            // }
            publishers common_publishers(allVars)
            publishers slack_publisher()
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
                        dslFactory.readFileFromWorkspace("dataeng/resources/prefect-flows-deployment.sh")
                    )
                }
            }
        }
    }
}
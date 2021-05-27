package analytics

import static org.edx.jenkins.dsl.AnalyticsConstants.common_authorization
import static org.edx.jenkins.dsl.AnalyticsConstants.common_log_rotator
import static org.edx.jenkins.dsl.AnalyticsConstants.common_wrappers
import static org.edx.jenkins.dsl.AnalyticsConstants.secure_scm
import static org.edx.jenkins.dsl.AnalyticsConstants.secure_scm_parameters


class PipelineAcceptanceTestManual {

    public static def job = { dslFactory, allVars ->
        dslFactory.job("edx-analytics-pipeline-acceptance-test-manual"){

            description(
                'This job is used for manually running acceptance tests for the edx-analytics pipeline, ' +
                'in the absence of automated CI on that repository.'
            )
            authorization common_authorization(allVars)
            parameters {
                stringParam('TASKS_REPO', allVars.get('TASKS_REPO'), '')
                stringParam('TASKS_BRANCH', allVars.get('TASKS_BRANCH'), '')
                stringParam('EXPORTER_BRANCH', allVars.get('EXPORTER_BRANCH'), '')
                stringParam('EXPORTER_BUCKET_PATH', allVars.get('EXPORTER_BUCKET_PATH'), '')
                textParam('ACCEPTANCE_TEST_CONFIG', allVars.get('ACCEPTANCE_TEST_CONFIG'), '')
                stringParam(
                    'ONLY_TESTS', allVars.get('ONLY_TESTS'),
                    'Only run the tests specified in this parameter (using nosetest path formatting, i.e. ' +
                    'path.to.some.file). If left blank, the entire suite will be run.'
                )
                stringParam('DISABLE_RESET_STATE', allVars.get('DISABLE_RESET_STATE'), '')
                stringParam('MAX_DIFF', allVars.get('MAX_DIFF'), '')
            }

            parameters secure_scm_parameters(allVars)
            multiscm secure_scm(allVars) << {
                git {
                    remote {
                        url('git@github.com:edx/edx-analytics-exporter.git')
                        branch('$EXPORTER_BRANCH')
                    }
                    extensions {
                        pruneBranches()
                        relativeTargetDirectory('analytics-exporter')
                    }
                }
                git {
                    remote {
                        url('$TASKS_REPO')
                        branch('$TASKS_BRANCH')
                    }
                    extensions {
                        relativeTargetDirectory('analytics-tasks')
                        pruneBranches()
                        cleanAfterCheckout()
                    }
                }
            }

            concurrentBuild(true)
            wrappers common_wrappers(allVars)

            steps {
                shell(dslFactory.readFileFromWorkspace("dataeng/resources/run-pipeline-acceptance-test.sh"))
            }

            publishers {
                archiveArtifacts {
                    pattern('build')
                    allowEmpty()
                }
                archiveJunit('analytics-tasks/nosetests.xml') {
                    allowEmptyResults(true)
                }
            }
        }
    }
}

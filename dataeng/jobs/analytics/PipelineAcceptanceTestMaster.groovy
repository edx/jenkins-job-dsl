package analytics

import static org.edx.jenkins.dsl.AnalyticsConstants.common_log_rotator
import static org.edx.jenkins.dsl.AnalyticsConstants.common_publishers
import static org.edx.jenkins.dsl.AnalyticsConstants.common_triggers
import static org.edx.jenkins.dsl.AnalyticsConstants.common_wrappers
import static org.edx.jenkins.dsl.AnalyticsConstants.secure_scm
import static org.edx.jenkins.dsl.AnalyticsConstants.secure_scm_parameters


class PipelineAcceptanceTestMaster {

    public static def job = { dslFactory, allVars ->
        dslFactory.job("edx-analytics-pipeline-acceptance-test-master"){

            description(
                'This job is used to run acceptance tests on the master branch of ' +
                'the edx-analytics-pipeline repo. It regularly polls that branch ' +
                'and will trigger a new build when a new commit is detected'
            )
            parameters {
                textParam('ACCEPTANCE_TEST_CONFIG', allVars.get('ACCEPTANCE_TEST_CONFIG'), '')
                stringParam('NOTIFY', '$PAGER_NOTIFY', 'Space separated list of emails to send notifications to.')
            }

            parameters secure_scm_parameters(allVars)
            multiscm secure_scm(allVars) << {
                git {
                    remote {
                        url('git@github.com:edx/edx-analytics-pipeline.git')
                        branch('origin/master')
                    }
                    extensions {
                        relativeTargetDirectory('analytics-tasks')
                        perBuildTag()
                    }
                }
                git {
                    remote {
                        url('git@github.com:edx/edx-analytics-exporter.git')
                        branch('origin/master')
                    }
                    extensions {
                        relativeTargetDirectory('analytics-exporter')
                        cleanAfterCheckout()
                        pruneBranches()
                        perBuildTag()
                    }
                }
            }

            environmentVariables {
                env('EXPORTER_BUCKET_PATH', allVars.get('EXPORTER_BUCKET_PATH'))
            }
            concurrentBuild(true)
            wrappers common_wrappers(allVars)

            triggers {
                pollSCM {
                    scmpoll_spec(allVars.get('JOB_FREQUENCY'))
                }
            }

            steps {
                shell(dslFactory.readFileFromWorkspace("dataeng/resources/run-pipeline-acceptance-test.sh"))
            }

            publishers common_publishers(allVars) << {
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

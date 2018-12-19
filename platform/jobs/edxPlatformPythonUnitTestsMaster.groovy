package platform

import org.yaml.snakeyaml.Yaml
import static org.edx.jenkins.dsl.JenkinsPublicConstants.GENERAL_PRIVATE_JOB_SECURITY
import static org.edx.jenkins.dsl.JenkinsPublicConstants.JENKINS_PUBLIC_LOG_ROTATOR
import static org.edx.jenkins.dsl.JenkinsPublicConstants.JENKINS_PUBLIC_JUNIT_REPORTS
import static org.edx.jenkins.dsl.JenkinsPublicConstants.GENERAL_SLACK_STATUS
import static org.edx.jenkins.dsl.JenkinsPublicConstants.JENKINS_PUBLIC_BASE_URL
import static org.edx.jenkins.dsl.JenkinsPublicConstants.JENKINS_PUBLIC_GITHUB_STATUS_SUCCESS
import static org.edx.jenkins.dsl.JenkinsPublicConstants.JENKINS_PUBLIC_GITHUB_STATUS_UNSTABLE_OR_WORSE


/* stdout logger */
Map config = [:]
Binding bindings = getBinding()
config.putAll(bindings.getVariables())
PrintStream out = config['out']

// This script generates a lot of jobs. Here is the breakdown of the configuration options:
// Map exampleConfig = [
//     open: true/false if this job should be 'open' (use the default security scheme or not)
//     jobName: name of the job
//     flowWorkerLabel: name of worker to run the flow job on
//     subsetjob: name of subset job run by this job (shard jobs)
//     repoName: name of the github repo containing the edx-platform you want to test
//     runCoverage: whether or not the shards should run unit tests through coverage, and then
//         run the coverage job on the results
//     coverageJob: name of the coverage job to run after the unit tests
//     workerLabel: label of the worker to run the subset jobs on
//     context: Github context used to report test status
//     targetBranch: branch of the edx-platform used as a comparison when running coverage.
//         This value is passed from the python job to the coverage job and used as an environment
//         variable
//     defaultTestengBranch: default branch of the testeng-ci repo for this job
//     refSpec: refspec for branches to build
//     defaultBranch: branch to build
// ]

Map publicJobConfig = [
    open: true,
    jobName: 'edx-platform-python-unittests-master',
    flowWorkerLabel: 'flow-worker-python',
    subsetJob: 'edx-platform-test-subset',
    repoName: 'edx-platform',
    runCoverage: true,
    coverageJob: 'edx-platform-unit-coverage',
    workerLabel: 'jenkins-worker',
    context: 'jenkins/python',
    targetBranch: 'origin/master',
    defaultTestengBranch: 'master',
    refSpec : '+refs/heads/master:refs/remotes/origin/master',
    defaultBranch : 'master'
]

Map hawthornJobConfig = [
    open: true,
    jobName: 'hawthorn-python-unittests-master',
    flowWorkerLabel: 'flow-worker-python',
    subsetJob: 'edx-platform-test-subset',
    repoName: 'edx-platform',
    runCoverage: true,
    coverageJob: 'edx-platform-unit-coverage',
    workerLabel: 'hawthorn-jenkins-worker',
    context: 'jenkins/hawthorn/python',
    targetBranch: 'origin/open-release/hawthorn.master',
    defaultTestengBranch : 'refs/heads/open-release/hawthorn.master',
    refSpec : '+refs/heads/open-release/hawthorn.master:refs/remotes/origin/open-release/hawthorn.master',
    defaultBranch : 'refs/heads/open-release/hawthorn.master'
]

Map ginkgoJobConfig = [
    open: true,
    jobName: 'ginkgo-python-unittests-master',
    flowWorkerLabel: 'flow-worker-python',
    subsetJob: 'edx-platform-test-subset',
    repoName: 'edx-platform',
    runCoverage: true,
    coverageJob: 'edx-platform-unit-coverage',
    workerLabel: 'ginkgo-jenkins-worker',
    context: 'jenkins/ginkgo/python',
    targetBranch: 'origin/open-release/ginkgo.master',
    defaultTestengBranch : 'refs/heads/open-release/ginkgo.master',
    refSpec : '+refs/heads/open-release/ginkgo.master:refs/remotes/origin/open-release/ginkgo.master',
    defaultBranch : 'refs/heads/open-release/ginkgo.master'
]

Map ficusJobConfig = [
    open: true,
    jobName: 'ficus-python-unittests-master',
    flowWorkerLabel: 'flow-worker-python',
    subsetJob: 'edx-platform-test-subset',
    repoName: 'edx-platform',
    runCoverage: true,
    coverageJob: 'edx-platform-unit-coverage',
    workerLabel: 'ficus-jenkins-worker',
    context: 'jenkins/ficus/python',
    targetBranch: 'origin/open-release/ficus.master',
    defaultTestengBranch : 'refs/heads/open-release/ficus.master',
    refSpec : '+refs/heads/open-release/ficus.master:refs/remotes/origin/open-release/ficus.master',
    defaultBranch : 'refs/heads/open-release/ficus.master'
]

List jobConfigs = [
    publicJobConfig,
    hawthornJobConfig,
    ginkgoJobConfig,
    ficusJobConfig
]

jobConfigs.each { jobConfig ->

    buildFlowJob(jobConfig.jobName) {

        if (!jobConfig.open.toBoolean()) {
            authorization GENERAL_PRIVATE_JOB_SECURITY()
        }
        properties {
            githubProjectUrl("https://github.com/edx/${jobConfig.repoName}/")
        }
        logRotator JENKINS_PUBLIC_LOG_ROTATOR(7)
        concurrentBuild()
        label(jobConfig.flowWorkerLabel)
        checkoutRetryCount(5)
        environmentVariables {
            env('SUBSET_JOB', jobConfig.subsetJob)
            env('REPO_NAME', jobConfig.repoName)
            env('RUN_COVERAGE', jobConfig.runCoverage)
            env('COVERAGE_JOB', jobConfig.coverageJob)
            env('TARGET_BRANCH', jobConfig.targetBranch)
        }
        parameters {
            stringParam('WORKER_LABEL', jobConfig.workerLabel, 'Jenkins worker for running the test subset jobs')
        }
        multiscm {
            git {
                remote {
                    url("git@github.com:edx/${jobConfig.repoName}.git")
                    refspec(jobConfig.refSpec)
                    credentials('jenkins-worker')
                }
                branch(jobConfig.defaultBranch)
                browser()
                extensions {
                    cloneOptions {
                        // Use a reference clone for quicker clones. This is configured on jenkins workers via
                        // (https://github.com/edx/configuration/blob/master/playbooks/roles/test_build_server/tasks/main.yml#L26)
                        reference("\$HOME/edx-platform-clone")
                        timeout(10)
                    }
                    cleanBeforeCheckout()
                    relativeTargetDirectory(jobConfig.repoName)
                }
            }
            git {
                remote {
                    url('https://github.com/edx/testeng-ci.git')
                }
                branch(jobConfig.defaultTestengBranch)
                browser()
                extensions {
                    cleanBeforeCheckout()
                    relativeTargetDirectory('testeng-ci')
                }
            }
        }
        triggers { githubPush() }
        wrappers {
            timestamps()
            sshAgent('jenkins-worker')
        }

        Map <String, String> predefinedPropsMap  = [:]
        predefinedPropsMap.put('GIT_SHA', '${GIT_COMMIT}')
        predefinedPropsMap.put('GITHUB_ORG', 'edx')
        predefinedPropsMap.put('CONTEXT', jobConfig.context)

        dslFile('testeng-ci/jenkins/flow/master/edx-platform-python-unittests-master.groovy')
        publishers {
            archiveJunit(JENKINS_PUBLIC_JUNIT_REPORTS)
            if (jobConfig.runCoverage) {
                configure { node ->
                    node / publishers << 'jenkins.plugins.shiningpanda.publishers.CoveragePublisher' {
                    }
                }
            }
            predefinedPropsMap.put('GITHUB_REPO', jobConfig.repoName)
            predefinedPropsMap.put('TARGET_URL', JENKINS_PUBLIC_BASE_URL +
                                   'job/' + jobConfig.jobName + '/${BUILD_NUMBER}/')
            downstreamParameterized JENKINS_PUBLIC_GITHUB_STATUS_SUCCESS.call(predefinedPropsMap)
            downstreamParameterized JENKINS_PUBLIC_GITHUB_STATUS_UNSTABLE_OR_WORSE.call(predefinedPropsMap)
            mailer('testeng@edx.org')
            configure GENERAL_SLACK_STATUS()
        }
    }
}

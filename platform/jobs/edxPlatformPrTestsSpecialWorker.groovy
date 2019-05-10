package platform

import static org.edx.jenkins.dsl.JenkinsPublicConstants.JENKINS_PUBLIC_LOG_ROTATOR

buildFlowJob('edx-platform-pr-tests-special-worker') {

    description('a wrapper job used to run a platform PR test suite on a given worker type')
    logRotator JENKINS_PUBLIC_LOG_ROTATOR(3)
    label('flow-worker')
    disabled()
    List<String> prJobs = [ 'edx-platform-accessibility-pr',
                            'edx-platform-bok-choy-pr',
                            'edx-platform-js-pr',
                            'edx-platform-lettuce-pr',
                            'edx-platform-python-unittests-pr',
                            'edx-platform-quality-pr' ]

    parameters {

        // Supply `ghprbActualCommit` to the jobs triggered by this wrapper job
        // `ghprbActualCommit` is an environment variable normally supplied via
        // the ghprb, but we can override it
        stringParam('ghprbActualCommit', '*/master', 'The hash of git commit to run downstream jobs on')
        // The label to run
        // Note: this label will be used to specify the worker on the following jobs:
        //  - edx-platform-(accessibility|js|quality)-pr => where the tests are actually run
        //      by the job itself
        //  - test-subset => again, tests are run here
        // It will NOT be used run on the following (but will pass the variable along to
        // test-subset jobs):
        //  - edx-platform-(bok-choy|lettuce-python-unittests)-pr => this is a flow job and does not
        //      actually run any test
        stringParam('WORKER_LABEL', 'jenkins-worker', 'The Jenkins worker type should be used to run this job')
        choiceParam('JOB_TO_RUN', prJobs, 'The platform job to run, given the hash & worker')
    }

    wrappers {
        timeout {
            absolute(80)
        }
        timestamps()
        colorizeOutput('gnome-terminal')
        buildName('#\${BUILD_NUMBER}: \${JOB_TO_RUN}')
    }

    String dslScript = readFileFromWorkspace('platform/resources/runJobOnWorker.groovy')
    buildFlow(dslScript)

}

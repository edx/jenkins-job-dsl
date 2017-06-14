package testeng

import static org.edx.jenkins.dsl.JenkinsPublicConstants.JENKINS_PUBLIC_TEAM_SECURITY

/* loadtestDriver.groovy
 *
 * Unlike most other jenkins jobs, loadtest-driver progressively refines its
 * results by continuous sampling.  Currently there is no tooling around
 * determining if a statistically significant amount data has been collected,
 * (not impossible to build, but it would likely depend on many external
 * factors).  Thus, this job relies on a human to manually abort it, or to set
 * the MAX_RUN_TIME parameter.
 *
 * Instructions:
 *
 * 1. Ensure the "jenkins worker loadtest driver" AMI has been prepared (from
 *    the jenkins_worker_loadtest.json packer script in edx/configuration).
 * 2. On jenkins master, manually configure a new jenkins worker:  Manage
 *    Jenkins -> Configure System -> Cloud -> Amazon EC2 -> Add.
 *
 *        Description: loadtest-driver-worker
 *        AMI ID: <use the AMI from step 1>
 *        Instance Type: M3Medium
 *        Security group names: jenkins-test
 *        Remote FS root: /home/jenkins
 *        Remote user: jenkins
 *        AMI Type: unix
 *        Labels: loadtest-driver-worker
 *        Usage: Only build jobs with label restrictions matching this node
 *        Idle termination time: 120
 *
 * 3. On jenkins master, manually upload a credential file called
 *    "loadtest_basic_auth_settings.yml" containing the current basic auth
 *    credentials in the second YAML document of that file: Credentials ->
 *    System -> Global credentials -> Add Credentials.
 *
 *        Scope: Global
 *        ID: loadtest_basic_auth_settings.yml
 */

/* Define job parameters centrally so that they can be reused across two jobs
 * (run-simple-loadtest and loadtest-driver).
 */
stringParams = [
    [
    name: 'TARGET_URL',
    description: 'The loadtest target where requests are to be sent.',
    default: 'https://courses-loadtest.edx.org'
    ],
    [
    name: 'TEST_COMPONENT',
    description: 'The desired component to loadtest. See complete list here: ' +
                 'https://github.com/edx/edx-load-tests/tree/master/loadtests',
    default: 'lms'
    ],
    [
    name: 'REMOTE_BRANCH',
    description: 'Branch of the edx-load-tests repo to use.',
    default: 'master'
    ],
    [
    name: 'NUM_CLIENTS',
    description: 'This many locust clients (i.e. fake users) will be hatched.',
    default: '500'
    ],
    [
    name: 'HATCH_RATE',
    description: 'Locust clients will be hatched at this rate ' +
                 '(hatches/second).',
    default: '30'
    ],
    [
    name: 'MAX_RUN_TIME',
    description: 'After this amount of time the loadtest will ' +
                 'automatically stop.  Its value is a floating point ' +
                 'number with an optional suffix: \'s\' for seconds (the ' +
                 'default), \'m\' for minutes, \'h\' for hours or \'d\' ' +
                 'for days.  The timer starts at the beginning of the ' +
                 'hatching phase. If left empty, the loadtest will never ' +
                 'stop itself.',
    default: '15m'
    ]
]
textParams = [
    [
    name: 'LOADTEST_OVERRIDES',
    description: 'Override the default settings. This YAML stream ' +
                 'should use the standard edx-load-test settings format. To ' +
                 'see the current defaults for $TEST_COMPONENT, refer to ' +
                 'the $TEST_COMPONENT.yml.example file at ' +
                 '<a href="https://github.com/edx/edx-load-tests/tree/master/settings_files">https://github.com/edx/edx-load-tests/tree/master/settings_files</a>.<br><br>' +
                 'DO NOT INCLUDE SENSITIVE OR LONG-TERM SECRETS.',
    default: null
    ]
]

/* This is the main top level job that humans should use to kick off a simple
 * load test.
 */
buildFlowJob('run-simple-loadtest') {
    description(
        'Start a new load test, and generate a summary report.<br><br>' +
        'Before kicking off a load test against the loadtest environment ' +
        '(courses-loadtest.edx.org) using this job, make sure to consult the ' +
        '<a href="https://openedx.atlassian.net/wiki/pages/viewpage.action?spaceKey=EdxOps&title=Loadtest+environment+queue">Loadtest environment queue</a>! ' +
        'For more information about running load tests, refer to the ' +
        '<a href="https://openedx.atlassian.net/wiki/display/EdxOps/How+to+Run+Performance+Tests">How to Run Performance Tests</a> ' +
        'wiki page.<br><br>' +
        'Stop a load test by aborting the downstream job "loadtest-driver". ' +
        'Aborting this job (run-simple-loadtest) will not stop load generation!'
    )

    /* Abusing the team security feature to give all job control
     * permissions to all edx employees.
     */
    authorization JENKINS_PUBLIC_TEAM_SECURITY.call(['edx'])

    parameters {
        stringParams.each { param ->
            stringParam(param.name, param.default, param.description)
        }
        textParams.each { param ->
            textParam(param.name, param.default, param.description)
        }
    }

    wrappers {
        buildUserVars() /* gives us access to BUILD_USER_ID, among other things */
        buildName('#${BUILD_NUMBER} by ${ENV,var="BUILD_USER_ID"}')
        timestamps()
        colorizeOutput('xterm')
    }

    buildFlow(readFileFromWorkspace(
        'testeng/resources/flow-run-simple-loadtest.groovy'
    ))
}

/* This is the job actually responsible for generating synthetic network load
 * against a remote.
 */
job('loadtest-driver') {
    description(
        'Create a new load test driver.<br><br>' +
        'If you are human, you should instead build the ' +
        '<a href="/job/run-simple-loadtest/">run-simple-loadtest job</a> ' +
        'to start a new load test.'
    )

    /* Abusing the team security feature to give all job control
     * permissions to all edx employees.
     */
    authorization JENKINS_PUBLIC_TEAM_SECURITY.call(['edx'])

    parameters {
        stringParams.each { param ->
            stringParam(param.name, param.default, param.description)
        }
        textParams.each { param ->
            textParam(param.name, param.default, param.description)
        }
    }

    /* It's okay to run multiple load tests at the same time.  We only require
     * that they are on different workers, and that constraint is configured
     * elsewhere.
     */
    concurrentBuild(true)

    /* This worker label corresponds to an AMI built using
     * util/packer/jenkins_worker_loadtest.json from edx/configuration.
     */
    label('loadtest-driver-worker')

    scm {
        git {
            remote {
                url('https://github.com/edx/edx-load-tests')
            }
            branch('\${REMOTE_BRANCH}')
            extensions {
                relativeTargetDirectory('edx-load-tests')
                cleanBeforeCheckout()
            }
        }
    }

    wrappers {
        buildUserVars() /* gives us access to BUILD_USER_ID, among other things */
        buildName('#${BUILD_NUMBER} by ${ENV,var="BUILD_USER_ID"}')
        timestamps()
        colorizeOutput('xterm')
        credentialsBinding {
            /* TODO: As of this writing there was no good repository to stick
             * per-loadtest-component secret settings, so currently we manually
             * install a generic secrets file containing the basic auth
             * settings directly into the jenkins master as a credentials file
             * named "loadtest_basic_auth_settings.yml".
             */
            file('SECRET_SETTINGS_FILE', 'loadtest_basic_auth_settings.yml')
        }
    }

    steps {
        shell(readFileFromWorkspace('testeng/resources/run-loadtest-wrapper.sh'))
    }

    publishers {
        archiveArtifacts {
            pattern('edx-load-tests/results/*')
        }
    }
}

job('loadtest-summary') {

    description('Summarize a past load test.')

    /* Abusing the team security feature to give all job control
     * permissions to all edx employees.
     */
    authorization JENKINS_PUBLIC_TEAM_SECURITY.call(['edx'])

    parameters {
        stringParam('REMOTE_BRANCH', 'master',
                    'Branch of the edx-load-tests repo to use.')
    }

    concurrentBuild(true)

    /* Build this on a loadtest-driver-worker instead of a generic jenkins worker because the former is likely already
     * online.
     */
    label('loadtest-driver-worker')

    scm {
        git {
            remote {
                url('https://github.com/edx/edx-load-tests')
            }
            branch('\${REMOTE_BRANCH}')
            extensions {
                relativeTargetDirectory('edx-load-tests')
                cleanBeforeCheckout()
                cloneOptions {
                    shallow(true)
                }
            }
        }
    }

    wrappers {
        buildUserVars() /* gives us access to BUILD_USER_ID, among other things */
        buildName('#${BUILD_NUMBER} by ${ENV,var="BUILD_USER_ID"}')
        timestamps()
        colorizeOutput('xterm')
    }

    steps {
        copyArtifacts('run-simple-loadtest') {
            buildSelector {
                /* Selects the upstream build that triggered this job as the build to copy artifacts from. */
                upstreamBuild {
                    /* Do not fallback to copying the artifacts from last successful upstream build, those are
                     * irrelevant because they correspond to a completely different load test.
                     */
                    fallbackToLastSuccessful(false)
                }
            }
            /* Do not ignore the directory structure of the artifacts */
            flatten(false)
        }
        virtualenv {
            name('edx-load-tests-venv')
            command('cd edx-load-tests && ' +
                    'pip install -e . && ' +
                    'generate_summary >results/summary.yml')
            clear(false)
        }
    }

    publishers {
        archiveArtifacts {
            pattern('edx-load-tests/results/summary.yml')
        }
    }
}

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

job('loadtest-driver') {

    description(
        'Start a new load test.<br><br>' +
        'Before kicking off a load test against the loadtest environment ' +
        '(courses-loadtest.edx.org) using this job, make sure to consult the ' +
        '<a href="https://openedx.atlassian.net/wiki/pages/viewpage.action?spaceKey=EdxOps&title=Loadtest+environment+queue">Loadtest environment queue</a>! ' +
        'For more information about running load tests, refer to the ' +
        '<a href="https://openedx.atlassian.net/wiki/display/EdxOps/How+to+Run+Performance+Tests">How to Run Performance Tests</a> ' +
        'wiki page.'
    )

    // Abusing the team security feature to give all job control
    // permissions to all edx employees.
    authorization JENKINS_PUBLIC_TEAM_SECURITY.call(['edx'])

    parameters {
        stringParam('TARGET_URL', 'https://courses-loadtest.edx.org',
                    'The loadtest target where requests are to be sent.')
        stringParam('TEST_COMPONENT', 'lms',
                    'The desired component to loadtest. See complete list here: ' +
                    'https://github.com/edx/edx-load-tests/tree/master/loadtests')
        stringParam('REMOTE_BRANCH', 'master',
                    'Branch of the edx-load-tests repo to use.')
        stringParam('NUM_CLIENTS', '500',
                    'This many locust clients (i.e. fake users) will be ' +
                    'hatched.')
        stringParam('HATCH_RATE', '30',
                    'Locust clients will be hatched at this rate ' +
                    '(hatches/second).')
        stringParam('MAX_RUN_TIME', '15m',
                    'After this amount of time the loadtest will ' +
                    'automatically stop.  Its value is a floating point ' +
                    'number with an optional suffix: \'s\' for seconds (the ' +
                    'default), \'m\' for minutes, \'h\' for hours or \'d\' ' +
                    'for days.  The timer starts at the beginning of the ' +
                    'hatching phase. If left empty, the loadtest will never ' +
                    'stop itself.')
        fileParam('job_param_overrides.yml',
                  'Override the default settings. This YAML file ' +
                  'should use the standard edx-load-test settings format. To ' +
                  'see the current defaults for $TEST_COMPONENT, refer to ' +
                  'the $TEST_COMPONENT.yml.example file at ' +
                  '<a href="https://github.com/edx/edx-load-tests/tree/master/settings_files">https://github.com/edx/edx-load-tests/tree/master/settings_files</a>.<br><br>' +
                  'DO NOT INCLUDE SENSITIVE SECRETS.')
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
        buildUserVars() // gives us access to BUILD_USER_ID, among other things
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
            pattern('edx-load-tests/results/log.txt')
        }
    }
}

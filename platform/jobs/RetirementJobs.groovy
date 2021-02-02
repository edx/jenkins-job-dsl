/* UserRetirementJobs.groovy
 *
 * Defines jobs which orchestrate user retirement.
 *
 * Prerequisites:
 *
 * 1. A global credential of type "secret file" with ID
 * "user-retirement-secure-default.yml".  The contents of this file is used by
 * default as the credentials configuration for all user retirement scripts.
 *
 * 2. A global credential of type "SSH username with private key" and ID
 * "jenkins-worker", used for checking out the secrets repo.
 *
 */

package platform

import org.yaml.snakeyaml.Yaml
import static org.edx.jenkins.dsl.JenkinsPublicConstants.JENKINS_PUBLIC_TEAM_SECURITY
import static org.edx.jenkins.dsl.JenkinsPublicConstants.JENKINS_PUBLIC_LOG_ROTATOR

/* stdout logger */
/* use this instead of println, because you can pass it into closures or other scripts. */
Map config = [:]
Binding bindings = getBinding()
config.putAll(bindings.getVariables())
PrintStream out = config['out']

/* Map to hold the k:v pairs parsed from the secret file */
Map mailingListMap = [:]
try {
    out.println('Parsing secret YAML file')
    String mailingListSecretContents  = new File("${MAILING_LIST_SECRET}").text
    Yaml yaml = new Yaml()
    mailingListMap = yaml.load(mailingListSecretContents)
    out.println('Successfully parsed secret YAML file')
}
catch (any) {
    out.println('Jenkins DSL: Error parsing secret YAML file')
    out.println('Exiting with error code 1')
    return 1
}

assert mailingListMap.containsKey('retirement_jobs_mailing_list')

// ########### user-retirement-driver ###########
// This defines the job for retiring individual users.
job('user-retirement-driver') {
    description('Drive the retirement of a single user which is ready for retirement immediately.')

    // Only a subset of edx employees should be allowed to control this job,
    // but customer support can read and discover.
    authorization {
        blocksInheritance(true)
        // Only core teams have full control of the retirement driver.
        List membersWithFullControl = ['edx*edx-data-engineering', 'edx*testeng', 'edx*devops']
        membersWithFullControl.each { emp ->
            permissionAll(emp)
        }
        // Educator is assisting with integration testing and validation, so
        // they need build access to run the retirement driver against one user
        // at a time.
        List extraMembersCanBuild = ['edx*educator-all']
        extraMembersCanBuild.each { emp ->
            permission('hudson.model.Item.Build', emp)
            permission('hudson.model.Item.Cancel', emp)
            permission('hudson.model.Item.Read', emp)
            permission('hudson.model.Item.Discover', emp)
        }
        // Other engineering teams can view.
        List extraMembersCanView = ['edx*learner', 'bbaker6225']
        extraMembersCanView.each { emp ->
            permission('hudson.model.Item.Read', emp)
            permission('hudson.model.Item.Discover', emp)
        }
        // TODO PLAT-2036: uncomment the following two lines when we add the
        // appropriate github group.
        //permission('hudson.model.Item.Read', 'edx/customer-support')
        //permission('hudson.model.Item.Discover', 'edx/customer-support')
    }

    // retirement-workers are configured to only execute one build at a time
    label('retirement-worker')

    // Allow this job to have simultaneous instances running at the same time
    // in general, but use the throttle-concurrents plugin to limit only one
    // instance of this job to run concurrently per environment per user.  This
    // would prevent race conditions related to triggering multiple retirement
    // driver jobs against the same user in the same environment.
    concurrentBuild(true)
    throttleConcurrentBuilds {
        // Tune this number to control the total number of simultaneous
        // retirements across all environments.  This does not accurately
        // throttle per environment, but we expect the vast majority of
        // retirement requests to come in through prod, so it's good enough for
        // now.
        maxTotal(4)
    }
    configure { project ->
        project / 'properties' / 'hudson.plugins.throttleconcurrents.ThrottleJobProperty' <<
            'paramsToUseForLimit'('ENVIRONMENT,RETIREMENT_USERNAME')
        project / 'properties' / 'hudson.plugins.throttleconcurrents.ThrottleJobProperty' <<
            'limitOneJobWithMatchingParams'('true')
    }

    // keep jobs around for 30 days
    logRotator JENKINS_PUBLIC_LOG_ROTATOR(30)

    wrappers {
        timeout {
            absolute(60)  // 1 hour
            failBuild()
        }
        buildUserVars() /* gives us access to BUILD_USER_ID, among other things */
        buildName('#${BUILD_NUMBER}, ${ENV,var="RETIREMENT_USERNAME"}')
        timestamps()
        colorizeOutput('xterm')
        credentialsBinding {
            file('USER_RETIREMENT_SECURE_DEFAULT', 'user-retirement-secure-default.yml')
        }
    }

    parameters {
        stringParam('TUBULAR_BRANCH', 'master', 'Repo branch for the tubular scripts.')
        stringParam('ENVIRONMENT', 'secure-default', 'edx environment which contains the user in question, in ENVIRONMENT-DEPLOYMENT format.')
        stringParam('RETIREMENT_USERNAME', '', 'Current username of learner to retire.')
    }

    // retry cloning repositories
    checkoutRetryCount(5)

    multiscm {
        git {
            remote {
                url('git@github.com:edx-ops/user-retirement-secure.git')
                credentials('jenkins-worker')
            }
            branch('master')
            extensions {
                relativeTargetDirectory('user-retirement-secure')
                cloneOptions {
                    shallow()
                    timeout(10)
                }
                cleanBeforeCheckout()
            }
        }
        git {
            remote {
                url('https://github.com/edx/tubular.git')
            }
            branch('$TUBULAR_BRANCH')
            extensions {
                relativeTargetDirectory('tubular')
                cloneOptions {
                    shallow()
                    timeout(10)
                }
                cleanBeforeCheckout()
            }
        }
    }

    environmentVariables {
        // Make sure that when we try to write unicode to the console, it
        // correctly encodes to UTF-8 rather than exiting with a UnicodeEncode
        // error.
        env('PYTHONIOENCODING', 'UTF-8')
        env('LC_CTYPE', 'en_US.UTF-8')
    }

    steps {
        shell(readFileFromWorkspace('platform/resources/user-retirement-driver.sh'))
    }

    publishers {
        // After all the build steps have completed, cleanup the workspace in
        // case this worker instance is re-used for a different job.
        wsCleanup()
    }
}


// ########### user-retirement-collector ###########
// This defines the "master" job for collecting users to retire.
job('user-retirement-collector') {
    description('Collect a group of users ready for retirement immediately, and trigger downstream ' +
                'user-retirement-driver jobs for each one.')

    // Only a subset of edx employees should be allowed to control this job,
    // but customer support can read and discover.
    authorization {
        blocksInheritance(true)
        // Only core teams can run the retirement collector directly.
        List membersWithFullControl = ['edx*edx-data-engineering', 'edx*testeng', 'edx*devops']
        membersWithFullControl.each { emp ->
            permissionAll(emp)
        }
        // Other engineering teams can view.
        List extraMembersCanView = ['edx*educator-all', 'edx*learner', 'bbaker6225']
        extraMembersCanView.each { emp ->
            permission('hudson.model.Item.Read', emp)
            permission('hudson.model.Item.Discover', emp)
        }
        // TODO PLAT-2036: uncomment the following two lines when we add the
        // appropriate github group.
        //permission('hudson.model.Item.Read', 'edx/customer-support')
        //permission('hudson.model.Item.Discover', 'edx/customer-support')
    }

    // retirement-workers are configured to only execute one build at a time
    label('retirement-worker')

    // Allow this job to have simultaneous builds at the same time in general,
    // but use the throttle-concurrents plugin to limit only one instance of
    // this job to run concurrently per environment.  This just helps keep
    // things simple.
    concurrentBuild(true)
    throttleConcurrentBuilds {
        // A maxTotal of 0 implies unlimited simultaneous jobs, but below we
        // restrict one build per environment.
        maxTotal(0)
    }
    configure { project ->
        project / 'properties' / 'hudson.plugins.throttleconcurrents.ThrottleJobProperty' <<
            'paramsToUseForLimit'('ENVIRONMENT')
        project / 'properties' / 'hudson.plugins.throttleconcurrents.ThrottleJobProperty' <<
            'limitOneJobWithMatchingParams'('true')
    }

    // keep jobs around for 30 days
    logRotator JENKINS_PUBLIC_LOG_ROTATOR(30)

    wrappers {
        timeout {
            absolute(60*24)  // 24 hours
            failBuild()
        }
        buildUserVars() /* gives us access to BUILD_USER_ID, among other things */
        buildName('#${BUILD_NUMBER}, ${ENV,var="ENVIRONMENT"}')
        timestamps()
        colorizeOutput('xterm')
        credentialsBinding {
            file('USER_RETIREMENT_SECURE_DEFAULT', 'user-retirement-secure-default.yml')
        }
    }

    parameters {
        stringParam('TUBULAR_BRANCH', 'master', 'Repo branch for the tubular scripts.')
        stringParam('ENVIRONMENT', 'secure-default', 'edx environment which contains the user in question, in ENVIRONMENT-DEPLOYMENT format.')
        stringParam('COOL_OFF_DAYS', '7', 'Number of days a learner should be in the retirement queue before being actually retired.')
        stringParam('USER_COUNT_ERROR_THRESHOLD', '200', 'If more users than this number are returned we will error out instead of retiring.')
    }

    // retry cloning repositories
    checkoutRetryCount(5)

    multiscm {
        git {
            remote {
                url('git@github.com:edx-ops/user-retirement-secure.git')
                credentials('jenkins-worker')
            }
            branch('master')
            extensions {
                relativeTargetDirectory('user-retirement-secure')
                cloneOptions {
                    shallow()
                    timeout(10)
                }
                cleanBeforeCheckout()
            }
        }
        git {
            remote {
                url('https://github.com/edx/tubular.git')
            }
            branch('$TUBULAR_BRANCH')
            extensions {
                relativeTargetDirectory('tubular')
                cloneOptions {
                    shallow()
                    timeout(10)
                }
                cleanBeforeCheckout()
            }
        }
    }

    environmentVariables {
        env('LEARNERS_TO_RETIRE_PROPERTIES_DIR', '${WORKSPACE}/learners-to-retire')

        // Make sure that when we try to write unicode to the console, it
        // correctly encodes to UTF-8 rather than exiting with a UnicodeEncode
        // error.
        env('PYTHONIOENCODING', 'UTF-8')
        env('LC_CTYPE', 'en_US.UTF-8')
    }

    steps {
        // This step calls out to the LMS and collects a list of learners to
        // retire.  The output is several generated properties files, one per
        // learner.
        shell(readFileFromWorkspace('platform/resources/user-retirement-collector.sh'))
        // This takes as input the properties files created in the previous
        // step, and triggers user-retirement-driver jobs per file.
        downstreamParameterized {
            trigger('user-retirement-driver') {
                // This section causes the build to block on completion of downstream builds.
                block {
                    // Mark this build step as FAILURE if at least one of the downstream builds were marked FAILED.
                    buildStepFailure('FAILURE')
                    // Mark this entire build as FAILURE if at least one of the downstream builds were marked FAILED.
                    failure('FAILURE')
                    // Mark this entire build as UNSTABLE if at least one of the downstream builds were marked UNSTABLE.
                    unstable('UNSTABLE')
                }
                parameters {
                    predefinedProp('TUBULAR_BRANCH', '${TUBULAR_BRANCH}')
                    predefinedProp('ENVIRONMENT', '${ENVIRONMENT}')
                }
                parameterFactories {
                    // This is Dynamic DSL magic that I copied from https://issues.jenkins-ci.org/browse/JENKINS-34552
                    fileBuildParameterFactory {
                       filePattern('learners-to-retire/*')
                       encoding('UTF-8')
                       noFilesFoundAction('SKIP')
                    }
                }
            }
        }
    }

    publishers {
        // After all the build steps have completed, cleanup the workspace in
        // case this worker instance is re-used for a different job.
        wsCleanup()

        // Send an alerting email upon failure.
        extendedEmail {
            recipientList(mailingListMap['retirement_jobs_mailing_list'])
            triggers {
                failure {
                    attachBuildLog(false)  // build log contains PII!
                    compressBuildLog(false)  // build log contains PII!
                    subject('Failed build: user-retirement-collector #${BUILD_NUMBER}')
                    content('Build #${BUILD_NUMBER} failed.\n\nSee ${BUILD_URL} for details.\n\nTo fix the failure, see https://openedx.atlassian.net/wiki/spaces/DE/pages/251495968/When+Jobs+Fail#WhenJobsFail-user-retirement-collector')
                    contentType('text/plain')
                    sendTo {
                        recipientList()
                    }
                }
            }
        }
    }
}

// ########### retirement-partner-reporter ###########
// Defines the job for running partner reporting
job('retirement-partner-reporter') {
    description('Run the partner reporting job and push the results to Google Drive.')

    authorization {
        blocksInheritance(true)
        // Only core teams have full control of the reporter.
        List membersWithFullControl = ['edx*edx-data-engineering', 'edx*testeng', 'edx*devops']
        membersWithFullControl.each { emp ->
            permissionAll(emp)
        }
        // Other engineering teams can view.
        List extraMembersCanView = ['edx*learner']
        extraMembersCanView.each { emp ->
            permission('hudson.model.Item.Read', emp)
            permission('hudson.model.Item.Discover', emp)
        }
    }

    // retirement-workers are configured to only execute one build at a time
    label('retirement-worker')

    // Only one of these jobs should be running at a time per environment
    concurrentBuild(true)
    throttleConcurrentBuilds {
        // A maxTotal of 0 implies unlimited simultaneous jobs, but below we
        // restrict one build per environment.
        maxTotal(0)
    }
    configure { project ->
        project / 'properties' / 'hudson.plugins.throttleconcurrents.ThrottleJobProperty' <<
            'paramsToUseForLimit'('ENVIRONMENT')
        project / 'properties' / 'hudson.plugins.throttleconcurrents.ThrottleJobProperty' <<
            'limitOneJobWithMatchingParams'('true')
    }

    // keep jobs around for 30 days
    logRotator JENKINS_PUBLIC_LOG_ROTATOR(30)

    wrappers {
        timeout {
            absolute(60*24)  // 24 hours
            failBuild()
        }
        buildUserVars() /* gives us access to BUILD_USER_ID, among other things */
        buildName('#${BUILD_NUMBER}, ${ENV,var="ENVIRONMENT"}')
        timestamps()
        colorizeOutput('xterm')
        credentialsBinding {
            file('USER_RETIREMENT_SECURE_DEFAULT', 'user-retirement-secure-default.yml')
        }
    }

    parameters {
        stringParam('TUBULAR_BRANCH', 'master', 'Repo branch for the tubular scripts.')
        stringParam('ENVIRONMENT', 'secure-default', 'edx environment which contains the user in question, in ENVIRONMENT-DEPLOYMENT format.')
    }

    // retry cloning repositories
    checkoutRetryCount(5)

    multiscm {
        git {
            remote {
                url('git@github.com:edx-ops/user-retirement-secure.git')
                credentials('jenkins-worker')
            }
            branch('master')
            extensions {
                relativeTargetDirectory('user-retirement-secure')
                cloneOptions {
                    shallow()
                    timeout(10)
                }
                cleanBeforeCheckout()
            }
        }
        git {
            remote {
                url('https://github.com/edx/tubular.git')
            }
            branch('$TUBULAR_BRANCH')
            extensions {
                relativeTargetDirectory('tubular')
                cloneOptions {
                    shallow()
                    timeout(10)
                }
                cleanBeforeCheckout()
            }
        }
    }

    environmentVariables {
        env('PARTNER_REPORTS_DIR', '${WORKSPACE}/partner-reports')

        // Make sure that when we try to write unicode to the console, it
        // correctly encodes to UTF-8 rather than exiting with a UnicodeEncode
        // error.
        env('PYTHONIOENCODING', 'UTF-8')
        env('LC_CTYPE', 'en_US.UTF-8')
    }

    steps {
        shell(readFileFromWorkspace('platform/resources/retirement-partner-reporter.sh'))
    }

    publishers {
        // After all the build steps have completed, cleanup the workspace in
        // case this worker instance is re-used for a different job.
        wsCleanup()

        // Send an alerting email upon failure.
        extendedEmail {
            recipientList(mailingListMap['retirement_jobs_mailing_list'])
            triggers {
                failure {
                    attachBuildLog(false)  // build log contains PII!
                    compressBuildLog(false)  // build log contains PII!
                    subject('Failed build: retirement-partner-reporter #${BUILD_NUMBER}')
                    content('Build #${BUILD_NUMBER} failed.\n\nSee ${BUILD_URL} for details.\n\nTo fix the failure, see https://openedx.atlassian.net/wiki/spaces/DE/pages/1077608460/Runbook+How+to+fix+a+failed+retirement-partner-reporter+run')
                    contentType('text/plain')
                    sendTo {
                        recipientList()
                    }
                }
            }
        }
    }
}

// ########### retirement-partner-report-cleanup ###########
// Defines the job for running partner reporting
job('retirement-partner-report-cleanup') {
    description('Run the partner report cleanup job.')

    authorization {
        blocksInheritance(true)
        // Only core teams have full control of the reporter.
        List membersWithFullControl = ['edx*edx-data-engineering', 'edx*testeng', 'edx*devops']
        membersWithFullControl.each { emp ->
            permissionAll(emp)
        }
        // Other engineering teams can view.
        List extraMembersCanView = ['edx*learner']
        extraMembersCanView.each { emp ->
            permission('hudson.model.Item.Read', emp)
            permission('hudson.model.Item.Discover', emp)
        }
    }

    // retirement-workers are configured to only execute one build at a time
    label('retirement-worker')

    // Only one of these jobs should be running at a time per environment
    concurrentBuild(true)
    throttleConcurrentBuilds {
        // A maxTotal of 0 implies unlimited simultaneous jobs, but below we
        // restrict one build per environment.
        maxTotal(0)
    }
    configure { project ->
        project / 'properties' / 'hudson.plugins.throttleconcurrents.ThrottleJobProperty' <<
            'paramsToUseForLimit'('ENVIRONMENT')
        project / 'properties' / 'hudson.plugins.throttleconcurrents.ThrottleJobProperty' <<
            'limitOneJobWithMatchingParams'('true')
    }

    // keep jobs around for 30 days
    logRotator JENKINS_PUBLIC_LOG_ROTATOR(30)

    wrappers {
        timeout {
            absolute(60*24)  // 24 hours
            failBuild()
        }
        buildUserVars() /* gives us access to BUILD_USER_ID, among other things */
        buildName('#${BUILD_NUMBER}, ${ENV,var="ENVIRONMENT"}')
        timestamps()
        colorizeOutput('xterm')
        credentialsBinding {
            file('USER_RETIREMENT_SECURE_DEFAULT', 'user-retirement-secure-default.yml')
        }
    }

    parameters {
        stringParam('TUBULAR_BRANCH', 'master', 'Repo branch for the tubular scripts.')
        stringParam('ENVIRONMENT', 'secure-default', 'edx environment which contains the user in question, in ENVIRONMENT-DEPLOYMENT format.')
        stringParam('AGE_IN_DAYS', '60', 'Number of days to keep partner reports.')
    }

    // retry cloning repositories
    checkoutRetryCount(5)

    multiscm {
        git {
            remote {
                url('git@github.com:edx-ops/user-retirement-secure.git')
                credentials('jenkins-worker')
            }
            branch('master')
            extensions {
                relativeTargetDirectory('user-retirement-secure')
                cloneOptions {
                    shallow()
                    timeout(10)
                }
                cleanBeforeCheckout()
            }
        }
        git {
            remote {
                url('https://github.com/edx/tubular.git')
            }
            branch('$TUBULAR_BRANCH')
            extensions {
                relativeTargetDirectory('tubular')
                cloneOptions {
                    shallow()
                    timeout(10)
                }
                cleanBeforeCheckout()
            }
        }
    }

    steps {
        shell(readFileFromWorkspace('platform/resources/retirement-partner-report-cleanup.sh'))
    }

    publishers {
        // After all the build steps have completed, cleanup the workspace in
        // case this worker instance is re-used for a different job.
        wsCleanup()

        // Send an alerting email upon failure.
        extendedEmail {
            recipientList(mailingListMap['retirement_jobs_mailing_list'])
            triggers {
                failure {
                    attachBuildLog(false)  // build log contains PII!
                    compressBuildLog(false)  // build log contains PII!
                    subject('Failed build: retirement-partner-report-cleanup #${BUILD_NUMBER}')
                    content('Build #${BUILD_NUMBER} failed.\n\nSee ${BUILD_URL} for details.')
                    contentType('text/plain')
                    sendTo {
                        recipientList()
                    }
                }
            }
        }
    }
}


// ########### user-retirement-archiver ###########
// This job was moved to the Tools Jenkins, see ../../devops/jobs/UserRetirementArchiver.groovy


// ########### user-retirement-bulk-status ###########
// This defines the retirement bulk status change job for users in the retirement queue.
job('user-retirement-bulk-status') {
    description('Moves learners in the retirement queue from one retirement state to another.')

    // Only a subset of edx employees should be allowed to control this job,
    // but customer support can read and discover.
    authorization {
        blocksInheritance(true)
        // Only core teams can run the retirement collector directly.
        List membersWithFullControl = ['edx*edx-data-engineering', 'edx*testeng', 'edx*devops']
        membersWithFullControl.each { emp ->
            permissionAll(emp)
        }
        // Other engineering teams can view.
        List extraMembersCanView = ['edx*educator-all', 'edx*learner']
        extraMembersCanView.each { emp ->
            permission('hudson.model.Item.Read', emp)
            permission('hudson.model.Item.Discover', emp)
        }
    }

    // retirement-workers are configured to only execute one build at a time
    label('retirement-worker')

    // Only one of these should run at a time.
    concurrentBuild(false)

    // keep jobs around for 30 days
    logRotator JENKINS_PUBLIC_LOG_ROTATOR(30)

    wrappers {
        buildUserVars() /* gives us access to BUILD_USER_ID, among other things */
        buildName('#${BUILD_NUMBER}, ${ENV,var="ENVIRONMENT"}')
        timestamps()
        colorizeOutput('xterm')
        credentialsBinding {
            file('USER_RETIREMENT_SECURE_DEFAULT', 'user-retirement-secure-default.yml')
        }
    }

    parameters {
        stringParam('TUBULAR_BRANCH', 'master', 'Repo branch for the tubular scripts.')
        stringParam('ENVIRONMENT', 'secure-default', 'edx environment which contains the user in question, in ENVIRONMENT-DEPLOYMENT format.')
        stringParam('START_DATE', '', 'Find users that requested deletion starting with this day (YYYY-MM-DD).')
        stringParam('END_DATE', '', 'Find users that requested deletion ending with this day (YYYY-MM-DD). To select one day make the start and end dates the same.')
        stringParam('INITIAL_STATE_NAME', '', 'Find retiring learners in this state (ex: COMPLETE)')
        stringParam('NEW_STATE_NAME', '', 'Set the found learners to this state (ex: PENDING)')
    }

    // retry cloning repositories
    checkoutRetryCount(5)

    multiscm {
        git {
            remote {
                url('git@github.com:edx-ops/user-retirement-secure.git')
                credentials('jenkins-worker')
            }
            branch('master')
            extensions {
                relativeTargetDirectory('user-retirement-secure')
                cloneOptions {
                    shallow()
                    timeout(10)
                }
                cleanBeforeCheckout()
            }
        }
        git {
            remote {
                url('https://github.com/edx/tubular.git')
            }
            branch('$TUBULAR_BRANCH')
            extensions {
                relativeTargetDirectory('tubular')
                cloneOptions {
                    shallow()
                    timeout(10)
                }
                cleanBeforeCheckout()
            }
        }
    }

    steps {
        // This step calls the shell script which talks to LMS
        shell(readFileFromWorkspace('platform/resources/user-retirement-bulk-status.sh'))
    }
    publishers {
        // After all the build steps have completed, cleanup the workspace in
        // case this worker instance is re-used for a different job.
        wsCleanup()

        // Send an alerting email upon failure.
        extendedEmail {
            recipientList(mailingListMap['retirement_jobs_mailing_list'])
            triggers {
                failure {
                    attachBuildLog(false)  // build log contains PII!
                    compressBuildLog(false)  // build log contains PII!
                    subject('Failed build: user-retirement-bulk-status #${BUILD_NUMBER}')
                    content('Build #${BUILD_NUMBER} failed.\n\nSee ${BUILD_URL} for details.')
                    contentType('text/plain')
                    sendTo {
                        recipientList()
                    }
                }
            }
        }
    }
}

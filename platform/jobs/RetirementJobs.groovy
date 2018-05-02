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

import static org.edx.jenkins.dsl.JenkinsPublicConstants.JENKINS_PUBLIC_TEAM_SECURITY
import static org.edx.jenkins.dsl.JenkinsPublicConstants.JENKINS_PUBLIC_LOG_ROTATOR


// ########### user-retirement-driver ###########
// This defines the job for retiring individual users.
job('user-retirement-driver') {
    description('Drive the retirement of a single user which is ready for retirement immediately.')

    // Only a subset of edx employees should be allowed to control this job,
    // but customer support can read and discover.
    authorization {
        blocksInheritance(true)
        List membersWithFullControl = ['edx/platform-team']
        membersWithFullControl.each { emp ->
            permissionAll(emp)
        }
        // TODO PLAT-2036: uncomment the following two lines when we add the
        // appropriate github group.
        //permission('hudson.model.Item.Read', 'edx/customer-support')
        //permission('hudson.model.Item.Discover', 'edx/customer-support')
    }

    // The jenkins-worker is intended for platform workers, but they'll work
    // well for this job, at least for now.  Specifically, this label is
    // configured to disallow more than one simultaneous job per instance, and
    // generally there are always available jenkins-worker instances idling.
    label('jenkins-worker')

    // Allow this job to have simultaneous instances building concurrently.
    // This will make it possible for retirement of multiple users to progress
    // in parallel.
    concurrentBuild(true)

    // This would provide a dial for throttling the retirements.  It is
    // currently commented out since the functionality is provided by the
    // throttle-concurrents plugin which we do not currently have installed in
    // Build Jenkins.
    //throttleConcurrentBuilds {
    //    maxTotal(2)
    //}

    // keep jobs around for 30 days
    logRotator JENKINS_PUBLIC_LOG_ROTATOR(30)

    wrappers {
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
        virtualenv {
            name('user-retirement-driver')
            nature('shell')
            command(readFileFromWorkspace('platform/resources/user-retirement-driver.sh'))
        }
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
        List membersWithFullControl = ['edx/platform-team']
        membersWithFullControl.each { emp ->
            permissionAll(emp)
        }
        // TODO PLAT-2036: uncomment the following two lines when we add the
        // appropriate github group.
        //permission('hudson.model.Item.Read', 'edx/customer-support')
        //permission('hudson.model.Item.Discover', 'edx/customer-support')
    }

    // The jenkins-worker is intended for platform workers, but they'll work
    // well for this job, at least for now.  Specifically, this label is
    // configured to disallow more than one simultaneous job per instance, and
    // generally there are always available jenkins-worker instances idling.
    label('jenkins-worker')

    // Disallow this job to have simultaneous instances building at the same
    // time.  This would prevent race conditions related to triggering multiple
    // retirement driver jobs against the same user.
    concurrentBuild(false)

    // keep jobs around for 30 days
    logRotator JENKINS_PUBLIC_LOG_ROTATOR(30)

    wrappers {
        buildUserVars() /* gives us access to BUILD_USER_ID, among other things */
        buildName('#${BUILD_NUMBER}')
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
    }

    triggers {
        // Build every hour, on the 15th minute (arbitrary).
        cron('15 * * * *')
    }

    // retry cloning repositories
    checkoutRetryCount(5)

    multiscm {
        git {
            remote {
                url('git@github.com:edx-ops/user-retirement-secure.git')
                credentials('jenkins-worker')
            }
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
    }

    steps {
        // This step calls out to the LMS and collects a list of learners to
        // retire.  The output is several generated properties files, one per
        // learner.
        virtualenv {
            name('user-retirement-collector')
            nature('shell')
            command(readFileFromWorkspace('platform/resources/user-retirement-collector.sh'))
        }
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
                       filePattern('${LEARNERS_TO_RETIRE_PROPERTIES_DIR}/*')
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
    }
}

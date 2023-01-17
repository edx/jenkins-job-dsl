package analytics

import static org.edx.jenkins.dsl.AnalyticsConstants.common_log_rotator
import static org.edx.jenkins.dsl.AnalyticsConstants.common_publishers
import static org.edx.jenkins.dsl.AnalyticsConstants.secure_scm
import static org.edx.jenkins.dsl.AnalyticsConstants.secure_scm_parameters
import static org.edx.jenkins.dsl.AnalyticsConstants.common_wrappers
import static org.edx.jenkins.dsl.AnalyticsConstants.common_authorization

class RetirementJobs{
    public static def job = { dslFactory, allVars ->

        // ########### user-retirement-driver ###########
        // This defines the job for retiring individual users.
        dslFactory.job('user-retirement-driver') {
            description('Drive the retirement of a single user which is ready for retirement immediately.')

            authorization common_authorization(allVars)

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
                maxTotal(10)
            }
            configure { project ->
                project / 'properties' / 'hudson.plugins.throttleconcurrents.ThrottleJobProperty' <<
                    'paramsToUseForLimit'('ENVIRONMENT,RETIREMENT_USERNAME')
                project / 'properties' / 'hudson.plugins.throttleconcurrents.ThrottleJobProperty' <<
                    'limitOneJobWithMatchingParams'('true')
            }

            // keep jobs around for 30 days
            // allVars contains the value for DAYS_TO_KEEP_BUILD which will be used inside AnalyticsConstants.common_log_rotator
            logRotator common_log_rotator(allVars)

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
                    usernamePassword('GITHUB_USER', 'GITHUB_TOKEN', 'GITHUB_USER_PASS_COMBO');
                }
            }
            wrappers common_wrappers(allVars)
            parameters secure_scm_parameters(allVars)
            parameters {
                stringParam('TUBULAR_BRANCH', 'master', 'Repo branch for the tubular scripts.')
                stringParam('ENVIRONMENT', '', 'edx environment which contains the user in question, in ENVIRONMENT-DEPLOYMENT format.')
                stringParam('RETIREMENT_USERNAME', '', 'Current username of learner to retire.')
            }

            // retry cloning repositories
            checkoutRetryCount(5)

            multiscm {
                git {
                    remote {
                        url('git@github.com:edx-ops/user-retirement-secure.git')
                        // using credentials from credentialsBinding.GITHUB_USER_PASS_COMBO
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
                shell(dslFactory.readFileFromWorkspace('dataeng/resources/user-retirement-driver.sh'))
            }

            publishers {
                // After all the build steps have completed, cleanup the workspace in
                // case this worker instance is re-used for a different job.
                wsCleanup()
            }
        }


        // ########### user-retirement-collector ###########
        // This defines the "master" job for collecting users to retire.
        dslFactory.job('user-retirement-collector') {
            description('Collect a group of users ready for retirement immediately, and trigger downstream ' +
                        'user-retirement-driver jobs for each one.')

            authorization common_authorization(allVars)

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
            // allVars contains the value for DAYS_TO_KEEP_BUILD which will be used inside AnalyticsConstants.common_log_rotator
            logRotator common_log_rotator(allVars)

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
                    usernamePassword('GITHUB_USER', 'GITHUB_TOKEN', 'GITHUB_USER_PASS_COMBO');
                }
            }
            parameters {
                stringParam('TUBULAR_BRANCH', 'master', 'Repo branch for the tubular scripts.')
                stringParam('ENVIRONMENT', '', 'edx environment which contains the user in question, in ENVIRONMENT-DEPLOYMENT format.')
                stringParam('COOL_OFF_DAYS', '14', 'Number of days a learner should be in the retirement queue before being actually retired.')
                stringParam('USER_COUNT_ERROR_THRESHOLD', '300', 'If more users than this number are returned we will error out instead of retiring.')
                stringParam('MAX_USER_BATCH_SIZE', '200', 'Allow us to get a specified number of users and then continues with that')
                stringParam('RETIREMENT_JOBS_MAILING_LIST', allVars.get('RETIREMENT_JOBS_MAILING_LIST'), 'Space separated list of emails to send notifications to.')
            }

            // retry cloning repositories
            checkoutRetryCount(5)

            multiscm {
                git {
                    remote {
                        url('git@github.com:edx-ops/user-retirement-secure.git')
                        // using credentials from credentialsBinding.GITHUB_USER_PASS_COMBO
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
                shell(dslFactory.readFileFromWorkspace('dataeng/resources/user-retirement-collector.sh'))
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
                    //recipientList(mailingListMap['retirement_jobs_mailing_list'])
                    recipientList(allVars.get('RETIREMENT_JOBS_MAILING_LIST'))
                    triggers {
                        failure {
                            attachBuildLog(false)  // build log contains PII!
                            compressBuildLog(false)  // build log contains PII!
                            subject('Build failed in Jenkins: user-retirement-collector #${BUILD_NUMBER}')
                            content('Build #${BUILD_NUMBER} failed.\n\nSee ${BUILD_URL} for details.\n\nTo fix the failure, see https://2u-internal.atlassian.net/wiki/spaces/DE/pages/10782098/When+Jobs+Fail#WhenJobsFail-user-retirement-collector')
                            contentType('text/plain')
                            sendTo {
                                recipientList()
                            }
                        }
                    }
                }

            }
        }

        // // ########### retirement-partner-reporter ###########
        // // Defines the job for running partner reporting
        dslFactory.job('retirement-partner-reporter') {
            description('Run the partner reporting job and push the results to Google Drive.')

            authorization common_authorization(allVars)

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
            // allVars contains the value for DAYS_TO_KEEP_BUILD which will be used inside AnalyticsConstants.common_log_rotator
            logRotator common_log_rotator(allVars)

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
                    usernamePassword('GITHUB_USER', 'GITHUB_TOKEN', 'GITHUB_USER_PASS_COMBO');
                }
            }

            parameters {
                stringParam('TUBULAR_BRANCH', 'master', 'Repo branch for the tubular scripts.')
                stringParam('ENVIRONMENT', '', 'edx environment which contains the user in question, in ENVIRONMENT-DEPLOYMENT format.')
                stringParam('RETIREMENT_JOBS_MAILING_LIST', allVars.get('RETIREMENT_JOBS_MAILING_LIST'), 'Space separated list of emails to send notifications to.')
            }

            // retry cloning repositories
            checkoutRetryCount(5)

            multiscm {
                git {
                    remote {
                        url('git@github.com:edx-ops/user-retirement-secure.git')
                        // using credentials from credentialsBinding.GITHUB_USER_PASS_COMBO
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
                shell(dslFactory.readFileFromWorkspace('dataeng/resources/retirement-partner-reporter.sh'))
            }

            publishers {
                // After all the build steps have completed, cleanup the workspace in
                // case this worker instance is re-used for a different job.
                wsCleanup()

                // Send an alerting email upon failure.
                extendedEmail {
                    //recipientList(mailingListMap['retirement_jobs_mailing_list'])
                    recipientList(allVars.get('RETIREMENT_JOBS_MAILING_LIST'))
                    triggers {
                        failure {
                            attachBuildLog(false)  // build log contains PII!
                            compressBuildLog(false)  // build log contains PII!
                            subject('Build failed in Jenkins: retirement-partner-reporter #${BUILD_NUMBER}')
                            content('Build #${BUILD_NUMBER} failed.\n\nSee ${BUILD_URL} for details.\n\nTo fix the failure, see https://2u-internal.atlassian.net/wiki/spaces/DE/pages/10780764/Runbook+How+to+fix+a+failed+retirement-partner-reporter+run')
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
        dslFactory.job('retirement-partner-report-cleanup') {
            description('Run the partner report cleanup job.')

            authorization common_authorization(allVars)

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
            // allVars contains the value for DAYS_TO_KEEP_BUILD which will be used inside AnalyticsConstants.common_log_rotator
            logRotator common_log_rotator(allVars)

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
                    usernamePassword('GITHUB_USER', 'GITHUB_TOKEN', 'GITHUB_USER_PASS_COMBO');
                }
            }

            parameters {
                stringParam('TUBULAR_BRANCH', 'master', 'Repo branch for the tubular scripts.')
                stringParam('ENVIRONMENT', '', 'edx environment which contains the user in question, in ENVIRONMENT-DEPLOYMENT format.')
                stringParam('AGE_IN_DAYS', '60', 'Number of days to keep partner reports.')
                stringParam('RETIREMENT_JOBS_MAILING_LIST', allVars.get('RETIREMENT_JOBS_MAILING_LIST'), 'Space separated list of emails to send notifications to.')
            }

            // retry cloning repositories
            checkoutRetryCount(5)

            multiscm {
                git {
                    remote {
                        url('git@github.com:edx-ops/user-retirement-secure.git')
                        // using credentials from credentialsBinding.GITHUB_USER_PASS_COMBO
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
                shell(dslFactory.readFileFromWorkspace('dataeng/resources/retirement-partner-report-cleanup.sh'))
            }

            publishers {
                // After all the build steps have completed, cleanup the workspace in
                // case this worker instance is re-used for a different job.
                wsCleanup()

                // Send an alerting email upon failure.
                extendedEmail {
                    //recipientList(mailingListMap['retirement_jobs_mailing_list'])
                    recipientList(allVars.get('RETIREMENT_JOBS_MAILING_LIST'))
                    triggers {
                        failure {
                            attachBuildLog(false)  // build log contains PII!
                            compressBuildLog(false)  // build log contains PII!
                            subject('Build failed in Jenkins: retirement-partner-report-cleanup #${BUILD_NUMBER}')
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



        // ########### user-retirement-bulk-status ###########
        // This defines the retirement bulk status change job for users in the retirement queue.
        dslFactory.job('user-retirement-bulk-status') {
            description('Moves learners in the retirement queue from one retirement state to another.')

            authorization common_authorization(allVars)

            // Only one of these should run at a time.
            concurrentBuild(false)

            // keep jobs around for 30 days
            // allVars contains the value for DAYS_TO_KEEP_BUILD which will be used inside AnalyticsConstants.common_log_rotator
            logRotator common_log_rotator(allVars)

            wrappers {
                buildUserVars() /* gives us access to BUILD_USER_ID, among other things */
                buildName('#${BUILD_NUMBER}, ${ENV,var="ENVIRONMENT"}')
                timestamps()
                colorizeOutput('xterm')
                credentialsBinding {
                    usernamePassword('GITHUB_USER', 'GITHUB_TOKEN', 'GITHUB_USER_PASS_COMBO');
                }
            }

            parameters {
                stringParam('TUBULAR_BRANCH', 'master', 'Repo branch for the tubular scripts.')
                stringParam('ENVIRONMENT', '', 'edx environment which contains the user in question, in ENVIRONMENT-DEPLOYMENT format.')
                stringParam('START_DATE', '', 'Find users that requested deletion starting with this day (YYYY-MM-DD).')
                stringParam('END_DATE', '', 'Find users that requested deletion ending with this day (YYYY-MM-DD). To select one day make the start and end dates the same.')
                stringParam('INITIAL_STATE_NAME', '', 'Find retiring learners in this state (ex: COMPLETE)')
                stringParam('NEW_STATE_NAME', '', 'Set the found learners to this state (ex: PENDING)')
                stringParam('RETIREMENT_JOBS_MAILING_LIST', allVars.get('RETIREMENT_JOBS_MAILING_LIST'), 'Space separated list of emails to send notifications to.')
            }

            // retry cloning repositories
            checkoutRetryCount(5)

            multiscm {
                git {
                    remote {
                        url('git@github.com:edx-ops/user-retirement-secure.git')
                        // using credentials from credentialsBinding.GITHUB_USER_PASS_COMBO
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
                shell(dslFactory.readFileFromWorkspace('dataeng/resources/user-retirement-bulk-status.sh'))
            }
            publishers {
                // After all the build steps have completed, cleanup the workspace in
                // case this worker instance is re-used for a different job.
                wsCleanup()

                // Send an alerting email upon failure.
                extendedEmail {
                    //recipientList(mailingListMap['retirement_jobs_mailing_list'])
                    recipientList(allVars.get('RETIREMENT_JOBS_MAILING_LIST'))
                    triggers {
                        failure {
                            attachBuildLog(false)  // build log contains PII!
                            compressBuildLog(false)  // build log contains PII!
                            subject('Build failed in Jenkins: user-retirement-bulk-status #${BUILD_NUMBER}')
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
    }
}

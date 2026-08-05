package analytics

import static org.edx.jenkins.dsl.AnalyticsConstants.common_log_rotator
import static org.edx.jenkins.dsl.AnalyticsConstants.common_publishers
import static org.edx.jenkins.dsl.AnalyticsConstants.secure_scm
import static org.edx.jenkins.dsl.AnalyticsConstants.secure_scm_parameters
import static org.edx.jenkins.dsl.AnalyticsConstants.common_wrappers
import static org.edx.jenkins.dsl.AnalyticsConstants.common_authorization
import static org.edx.jenkins.dsl.AnalyticsConstants.platform_scm

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
                    'paramsToUseForLimit'('ENVIRONMENT,RETIREMENT_USER_ID')
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
                buildName('#${BUILD_NUMBER}, ${ENV,var="RETIREMENT_USER_ID"}')
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
                stringParam('RETIREMENT_USER_ID', '', 'LMS user ID of the learner to retire.')
            }

            // retry cloning repositories
            checkoutRetryCount(5)

            multiscm {
                git {
                    remote {
                        url('https://github.com/edx/configuration.git')
                    }
                    branch('master')
                    extensions {
                        relativeTargetDirectory('configuration')
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
                stringParam('USER_COUNT_ERROR_THRESHOLD', '251', 'If more users than this number are returned we will error out instead of retiring.')
                stringParam('MAX_USER_BATCH_SIZE', '200', 'Allow us to get a specified number of users and then continues with that')
                stringParam('RETIREMENT_JOBS_MAILING_LIST', allVars.get('RETIREMENT_JOBS_MAILING_LIST'), 'Space separated list of emails to send notifications to.')
            }

            // retry cloning repositories
            checkoutRetryCount(5)

            multiscm {
                git {
                    remote {
                        url('https://github.com/edx/configuration.git')
                    }
                    branch('master')
                    extensions {
                        relativeTargetDirectory('configuration')
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
                            content('Build #${BUILD_NUMBER} failed.\n\nSee ${BUILD_URL} for details.\n\nTo fix the failure, see https://2u-internal.atlassian.net/wiki/spaces/AT/pages/2607644695/runbook+How+to+fix+a+failed+user-retirement-collector')
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
                stringParam('AGE_IN_DAYS', '60', 'Number of days to keep partner reports; should match retirement-partner-report-cleanup job AGE_IN_DAYS to keep retention and cleanup in sync.')
                stringParam('DELETION_WARNING_DAYS', '7', 'Number of days before deletion to send warning notification.')
                stringParam('RETIREMENT_JOBS_MAILING_LIST', allVars.get('RETIREMENT_JOBS_MAILING_LIST'), 'Space separated list of emails to send notifications to.')
            }

            // retry cloning repositories
            checkoutRetryCount(5)

            multiscm {
                git {
                    remote {
                        url('https://github.com/edx/configuration.git')
                    }
                    branch('master')
                    extensions {
                        relativeTargetDirectory('configuration')
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
                git {
                    remote {
                        url('git@github.com:edx/edx-internal.git')
                        credentials(allVars.get('SECURE_GIT_CREDENTIALS'))
                    }
                    branch('master')
                    extensions {
                        relativeTargetDirectory('edx-internal')
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
                            content('Build #${BUILD_NUMBER} failed.\n\nSee ${BUILD_URL} for details.\n\nTo fix the failure, see https://2u-internal.atlassian.net/wiki/spaces/ENG/pages/2357756586/Runbook+How+to+fix+a+failed+retirement-partner-reporter+run')
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
                        url('https://github.com/edx/configuration.git')
                    }
                    branch('master')
                    extensions {
                        relativeTargetDirectory('configuration')
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
                git {
                    remote {
                        url('git@github.com:edx/edx-internal.git')
                        credentials(allVars.get('SECURE_GIT_CREDENTIALS'))
                    }
                    branch('master')
                    extensions {
                        relativeTargetDirectory('edx-internal')
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
                booleanParam('REWIND_STATE', false, 'Rewinds users to previous state, useful for redriving a large numnber of ERRORED users')
            }

            // retry cloning repositories
            checkoutRetryCount(5)

            multiscm {
                git {
                    remote {
                        url('https://github.com/edx/configuration.git')
                    }
                    branch('master')
                    extensions {
                        relativeTargetDirectory('configuration')
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

        // ########### user-retirement-health-check ###########
        // This defines the stage-only synthetic health check for user retirement.
        def gitCredentialId = allVars.get('SECURE_GIT_CREDENTIALS')
        def healthCheckPasswordCredentialId = allVars.get(
            'RETIREMENT_HEALTH_CHECK_PASSWORD_CREDENTIAL_ID',
            'retirement-health-check-password'
        )
        def healthCheckDeployments = allVars.get(
            'USER_RETIREMENT_HEALTH_CHECK_DEPLOYMENTS',
            ['edx': ['environments': ['stage']]]
        )

        healthCheckDeployments.each { deployment, configuration ->
            configuration.get('environments').each { environment ->
                if (environment == 'stage') {

                    def environmentDeployment = "${environment}-${deployment}"
                    def jobName = "user-retirement-health-check-${deployment}-${environment}"

                    dslFactory.job(jobName) {
                        description('Run a stage-only synthetic user retirement health check and alert on regressions.')

                        authorization common_authorization(allVars)
                        concurrentBuild(false)

                        logRotator common_log_rotator(allVars)

                        wrappers {
                            timeout {
                                absolute(allVars.get('USER_RETIREMENT_HEALTH_CHECK_JOB_TIMEOUT_MINUTES', 90) as int)
                                failBuild()
                            }
                            buildUserVars()
                            buildName('#${BUILD_NUMBER}, ${ENV,var="ENVIRONMENT"}')
                            timestamps()
                            colorizeOutput('xterm')
                            credentialsBinding {
                                usernamePassword('GITHUB_USER', 'GITHUB_TOKEN', 'GITHUB_USER_PASS_COMBO')
                                string('RETIREMENT_HEALTH_CHECK_PASSWORD', healthCheckPasswordCredentialId)
                            }
                        }

                        parameters {
                            stringParam('CONFIGURATION_REPO', allVars.get('USER_RETIREMENT_HEALTH_CHECK_CONFIGURATION_REPO', 'git@github.com:edx/configuration.git'), 'Repo URL for edx/configuration.')
                            stringParam('CONFIGURATION_BRANCH', allVars.get('USER_RETIREMENT_HEALTH_CHECK_CONFIGURATION_BRANCH', 'master'), 'Repo branch for edx/configuration.')
                            stringParam('PLAYWRIGHT_E2E_REPO', allVars.get('USER_RETIREMENT_HEALTH_CHECK_PLAYWRIGHT_E2E_REPO', 'git@github.com:edx/playwright-e2e.git'), 'Repo URL for the playwright-e2e tests.')
                            stringParam('PLAYWRIGHT_E2E_BRANCH', allVars.get('USER_RETIREMENT_HEALTH_CHECK_PLAYWRIGHT_E2E_BRANCH', 'main'), 'Repo branch for the playwright-e2e tests.')
                            stringParam('TUBULAR_REPO', allVars.get('USER_RETIREMENT_HEALTH_CHECK_TUBULAR_REPO', 'git@github.com:edx/tubular.git'), 'Repo URL for the tubular scripts.')
                            stringParam('TUBULAR_BRANCH', allVars.get('USER_RETIREMENT_HEALTH_CHECK_TUBULAR_BRANCH', 'master'), 'Repo branch for the tubular scripts.')
                            stringParam('ENVIRONMENT', environmentDeployment, 'edx environment for the health check. MVP is stage-only.')
                            stringParam('RETIREMENT_JOBS_MAILING_LIST', allVars.get('RETIREMENT_JOBS_MAILING_LIST'), 'Space separated list of emails to send notifications to.')
                            stringParam('PYTHON_VERSION', allVars.get('USER_RETIREMENT_HEALTH_CHECK_PYTHON_VERSION', '3.9'), 'Python version to use for the health check virtualenv.')
                            stringParam('TIMEOUT_SECONDS', allVars.get('USER_RETIREMENT_HEALTH_CHECK_TIMEOUT_SECONDS', 1800).toString(), 'Polling timeout in seconds.')
                            stringParam('POLL_INTERVAL_SECONDS', allVars.get('USER_RETIREMENT_HEALTH_CHECK_POLL_INTERVAL_SECONDS', 30).toString(), 'Polling interval in seconds.')
                        }

                        checkoutRetryCount(5)

                        multiscm {
                            git {
                                remote {
                                    url('$CONFIGURATION_REPO')
                                    branch('$CONFIGURATION_BRANCH')
                                    credentials(gitCredentialId)
                                }
                                extensions {
                                    relativeTargetDirectory('configuration')
                                    cloneOptions {
                                        shallow()
                                        timeout(10)
                                    }
                                    cleanBeforeCheckout()
                                }
                            }
                            git {
                                remote {
                                    url('$PLAYWRIGHT_E2E_REPO')
                                    branch('$PLAYWRIGHT_E2E_BRANCH')
                                    credentials(gitCredentialId)
                                }
                                extensions {
                                    relativeTargetDirectory('playwright-e2e')
                                    cloneOptions {
                                        shallow()
                                        timeout(10)
                                    }
                                    cleanBeforeCheckout()
                                }
                            }
                            git {
                                remote {
                                    url('$TUBULAR_REPO')
                                    branch('$TUBULAR_BRANCH')
                                    credentials(gitCredentialId)
                                }
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
                            env('TEST_ENV', allVars.get('USER_RETIREMENT_HEALTH_CHECK_TEST_ENV', environment))
                            env('RUN_HEALTH_CHECK', allVars.get('USER_RETIREMENT_HEALTH_CHECK_RUN_HEALTH_CHECK', 'true'))
                            env('RETIREMENT_ARTIFACT_PATH', allVars.get('USER_RETIREMENT_HEALTH_CHECK_RETIREMENT_ARTIFACT_PATH', '${WORKSPACE}/playwright-e2e/retirement-health-check/artifact.json'))
                            env('RETIREMENT_CONFIG_FILE', allVars.get('USER_RETIREMENT_HEALTH_CHECK_RETIREMENT_CONFIG_FILE', 'stage-retirement.yml'))
                            env('TUBULAR_PATH', '${WORKSPACE}/tubular')
                            env('PYTHONIOENCODING', 'UTF-8')
                            env('LC_CTYPE', 'en_US.UTF-8')
                        }

                        steps {
                            shell(dslFactory.readFileFromWorkspace('dataeng/resources/user-retirement-health-check.sh'))
                        }

                        publishers {
                            archiveArtifacts {
                                pattern('playwright-e2e/retirement-health-check/artifact.json,playwright-e2e/playwright-report/**,playwright-e2e/test-results/**')
                                allowEmpty(true)
                            }
                            wsCleanup()

                            extendedEmail {
                                recipientList(allVars.get('RETIREMENT_JOBS_MAILING_LIST'))
                                triggers {
                                    failure {
                                        attachBuildLog(false)  // build log may contain learner data.
                                        compressBuildLog(false)
                                        subject('Build failed in Jenkins: ' + jobName + ' #${BUILD_NUMBER}')
                                        content('Build #${BUILD_NUMBER} failed.\n\nSee ${BUILD_URL} for details.\n\nRunbook: ' + allVars.get('USER_RETIREMENT_HEALTH_CHECK_RUNBOOK_URL', 'https://2u-internal.atlassian.net/wiki/spaces/AT/pages/2765422605/Django+Management+Command+Failure+Runbook'))
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
        }
    }
}

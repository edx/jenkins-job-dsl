/*
 * Common DSL closures for all User Retirement jobs.
 */
package org.edx.jenkins.dsl

class UserRetirementConstants {
    // All retirement jobs have two elevated levels of access control:
    //   1) read/build (determined by the ACCESS_CONTROL job config variable)
    //   2) admin (determined by the ADMIN_ACCESS_CONTROL job config variable)
    public static def common_access_controls = { extraVars ->
        def read_build_permission_names = [
            'hudson.model.Item.Build',
            'hudson.model.Item.Cancel',
            'hudson.model.Item.Discover',
            'hudson.model.Item.Read',
        ]
        def read_build_perms = extraVars.get('ACCESS_CONTROL',[]).each { acl ->
            read_build_permission_names.each { perm ->
                permission(perm,acl)
            }
        }
        def admin_perms = extraVars.get('ADMIN_ACCESS_CONTROL',[]).each { acl ->
            permissionAll(acl)
        }
        // The ">>" operator composes two closures into one closure.
        return read_build_perms >> admin_perms
    }

    public static def common_triggers = { extraVars ->
        if (extraVars.containsKey('CRON')) {
            cron(extraVars.get('CRON'))
        }
    }

    public static def common_wrappers = { extraVars ->
        return {
            buildUserVars() /* gives us access to BUILD_USER_ID, among other things */
            buildName('#${BUILD_NUMBER}, ' + extraVars.get('ENVIRONMENT_DEPLOYMENT'))
            timestamps()
            colorizeOutput('xterm')
        }
    }

    public static def common_parameters = { extraVars ->
        return {
            stringParam('TUBULAR_BRANCH', 'master', 'Repo branch for the tubular scripts.')
            stringParam('USER_RETIREMENT_SECURE_BRANCH', 'master', 'Repo branch for the tubular scripts.')
        }
    }

    public static def common_multiscm = { extraVars ->
        return {
            git {
                remote {
                    url('git@github.com:edx-ops/user-retirement-secure.git')
                    if (extraVars.containsKey('SECURE_GIT_CREDENTIALS')) {
                        credentials(extraVars.get('SECURE_GIT_CREDENTIALS'))
                    }
                }
                branch('$USER_RETIREMENT_SECURE_BRANCH')
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
    }
    public static def common_publishers = { extraVars ->
        return {
            // After all the build steps have completed, cleanup the workspace in
            // case this worker instance is re-used for a different job.
            wsCleanup()

            if (extraVars.containsKey('MAILING_LIST')) {
                // Send an alerting email upon failure.
                extendedEmail {
                    recipientList(extraVars.get('MAILING_LIST'))
                    triggers {
                        failure {
                            attachBuildLog(false)  // build log contains PII!
                            compressBuildLog(false)  // build log contains PII!
                            subject('Failed build: ${JOB_NAME} #${BUILD_NUMBER}')
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
    public static def common_closures_extra = { extraVars ->
        return {
            disabled(extraVars.get('DISABLED'))  // Jobs may be disabled for testing/rollout.
            checkoutRetryCount(5)  // Retry cloning repositories.
        }
    }
}

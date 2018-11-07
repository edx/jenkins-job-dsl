/*

Defines the job for archiving retired user statuses.

Variables consumed for this job:
  * MAILING_LIST: A string containing a comma-delimited list of emails to notify when a build fails.
  * SECURE_GIT_CREDENTIALS: Name of jenkins git credential scoped to the UserRetirement "folder" in jenkins.
  * FOLDER_NAME: Name of jenkins "folder".
  * ACCESS_CONTROL: List of github groups with read and build access to this job.
  * ADMIN_ACCESS_CONTROL: List of github groups with full admin access to this job.

This job expects the following credentials to be defined on the folder
  * tools-edx-jenkins-aws-credentials: file with key/secret in boto config format
  * tools-jenkins-janitor-${deployment}-role-arn: the role to aws sts assume-role

*/
package devops.jobs
import static org.edx.jenkins.dsl.DevopsConstants.common_read_permissions

class UserRetirementArchiver {
    public static def job = { dslFactory, extraVars ->

        // Job config assertions:
        assert (extraVars.get('MAILING_LIST', '') instanceof String) :
            "Make sure MAILING_LIST is a single string (containing a comma-delimited list) and not a proper Yaml " +
            "list or some other non-string type."

        dslFactory.job(extraVars.get("FOLDER_NAME") + "/user-retirement-archiver") {
            description('Archive the old user retirement status rows to AWS S3.')

            // Only one of these jobs should be running at a time.
            concurrentBuild(false)

            // keep jobs around for 30 days.
            logRotator {
                daysToKeep(30)
            }

            extraVars.get('ACCESS_CONTROL',[]).each { acl ->
                common_read_permissions.each { perm ->
                    authorization {
                        permission(perm,acl)
                    }
                }
            }

            extraVars.get('ADMIN_ACCESS_CONTROL',[]).each { acl ->
                authorization {
                    permissionAll(acl)
                }
            }

            wrappers {
                buildUserVars() /* gives us access to BUILD_USER_ID, among other things */
                buildName('#${BUILD_NUMBER}, ${ENV,var="ENVIRONMENT"}')
                timestamps()
                colorizeOutput('xterm')
                credentialsBinding {
                    file('AWS_CONFIG_FILE','tools-edx-jenkins-aws-credentials')
                    string('ROLE_ARN', "${environment}-${deployment}-retirement-archive-upload-role")
                }
            }

            def gitCredentialId = extraVars.get('SECURE_GIT_CREDENTIALS','')

            parameters {
                stringParam('TUBULAR_BRANCH', 'master', 'Repo branch for the tubular scripts.')
                stringParam('USER_RETIREMENT_SECURE_BRANCH', 'master', 'Repo branch for the tubular scripts.')
                stringParam('ENVIRONMENT', '', 'edx environment from which to archive user retirements, in ENVIRONMENT-DEPLOYMENT format. (Required)')
                stringParam('COOL_OFF_DAYS', '67', 'Number of days after retirement request in which a retired learner status should be archived in S3.')
            }

            // retry cloning repositories
            checkoutRetryCount(5)

            multiscm {
                git {
                    remote {
                        url('git@github.com:edx-ops/user-retirement-secure.git')
                        if (gitCredentialId) {
                            credentials(gitCredentialId)
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

            steps {
                virtualenv {
                    pythonName('System-CPython-3.5')
                    name('user-retirement-archiver')
                    nature('shell')
                    systemSitePackages(false)
                    command(readFileFromWorkspace('devops/resources/user-retirement-archiver.sh'))
                }
            }

            publishers {
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
                                subject('Failed build: user-retirement-archiver #${BUILD_NUMBER}')
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
}

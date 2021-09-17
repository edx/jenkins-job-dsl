/*

 This job checks out the app permissions repository and checks for merges to master with github webhooks. Upon a merge to master,
 it will trigger a build of the app-permissions-runner job (below). The app-permissions-runner job is what actually calls the script
 to run the ansible.

 Variables without defaults are marked (required)

 Variables consumed for this job and for the app-permission-runner job:
    * SECURE_GIT_CREDENTIALS: secure-bot-user (required)
    * FOLER_NAME: folder, default is User-Mananagement
    * DEPLOYMENTS: (required)
        environments:
          - environment (required)
    * ACCESS_CONTROL: list of users to give access to
        - user
    * USER: user to run ansible (required)
    * DEPLOYMENT_KEY: ssh key that should be defined on the folder (required)
    * NOTIFY_ON_FAILURE: alert@example.com
    * APP_PERMISSION_REPO: app permissions git repository (required)
    * APP_PERMISSION_BRANCH: branch of app permissions for the webhook to watch, default is master

*/


package devops.jobs
import static org.edx.jenkins.dsl.Constants.common_wrappers
import static org.edx.jenkins.dsl.Constants.common_logrotator
import static org.edx.jenkins.dsl.DevopsConstants.common_read_permissions
import static org.edx.jenkins.dsl.DevopsConstants.merge_to_master_trigger

class AppPermissionsWatcher{
    public static def job = { dslFactory, extraVars ->
        dslFactory.job(extraVars.get("FOLDER_NAME","App-Permissions") + "/app-permissions-watcher") {

            description("Job to watch the app-permissions repo to determine when to trigger the app-permissions-runner jobs.")

            wrappers common_wrappers
            logRotator common_logrotator

            def access_control = extraVars.get('ACCESS_CONTROL',[])
            access_control.each { acl ->
                common_read_permissions.each { perm ->
                    authorization {
                        permission(perm,acl)
                    }
                }
            }

            throttleConcurrentBuilds {
                maxPerNode(0)
                maxTotal(0)
            }

            def gitCredentialId = extraVars.get('SECURE_GIT_CREDENTIALS','')
            def repo_branch = extraVars.get('APP_PERMISSIONS_BRANCH', 'master')

            // The urls for the repos and the branch names cannot be variables that are defined in the scm checkout because
            // they are not defined until run time so the webhook cannot find them.
            // If you want to parameterize them, define them as variables within the job prior to the scm checkout (as done above)
            scm{
                git {
                    remote {
                        url('git@github.com:edx/app-permissions.git')
                        branch(repo_branch)
                            if (gitCredentialId) {
                                credentials(gitCredentialId)
                            }
                    }
                    extensions {
                        cleanAfterCheckout()
                        pruneBranches()
                        relativeTargetDirectory('app-permissions')
                    }
                }
            }

            properties {
                githubProjectUrl("https://github.com/edx/app-permissions/")
            }

            triggers merge_to_master_trigger(repo_branch)

            steps{
                extraVars.get('DEPLOYMENTS').each { deployment, configuration ->
                    configuration.environments.each { environments ->
                        environments.each { environment, jobtypes ->
                            jobtypes.each { jobType ->
                                downstreamParameterized {
                                    trigger("app-permissions-runner-${environment}-${deployment}-${jobType}")
                                }
                            }
                        }
                    }
                }
            }

        }
    }
}


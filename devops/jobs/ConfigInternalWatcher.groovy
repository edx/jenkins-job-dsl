/*

 This job checks out the ${deployment}-internal repository and checks for merges to master with github webhooks. Upon a merge to master,
 it will trigger a build of the bastion-access jobs (below). The bastion-access jobs are what actually calls the script
 to run the ansible.

 Variables without defaults are marked (required)

 Variables consumed for this job and for the bastion-access-* jobs:
    * SECURE_GIT_CREDENTIALS: secure-bot-user (required)
    * FOLER_NAME: folder, default is bastion_access
    * DEPLOYMENTS: (required)
        environments:
          - environment (required)
    * NOTIFY_ON_FAILURE: alert@example.com
    * CONFIGURATION_INTERNAL_BRANCH: branch of ${deployment}-internal for the webhook to watch, default is master

*/


package devops.jobs
import static org.edx.jenkins.dsl.Constants.common_wrappers
import static org.edx.jenkins.dsl.Constants.common_logrotator
import static org.edx.jenkins.dsl.DevopsConstants.common_read_permissions
import static org.edx.jenkins.dsl.DevopsConstants.merge_to_master_trigger

class ConfigInternalWatcher{
    public static def job = { dslFactory, extraVars ->
        dslFactory.job(extraVars.get("FOLDER_NAME","bastion_access") + "/bastion-access-watcher") {
            extraVars.get('DEPLOYMENTS').each { deployment, configuration ->

                description("Job to watch the configuration-internal repo to determine when to trigger the bastion-access jobs.")

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
                def repo_branch = extraVars.get('CONFIGURATION_INTERNAL_BRANCH', 'master')

                // The urls for the repos and the branch names cannot be variables that are defined in the scm checkout because
                // they are not defined until run time so the webhook cannot find them.
                // If you want to parameterize them, define them as variables within the job prior to the scm checkout (as done above)
                scm{
                    git {
                        remote {
                            url("git@github.com:edx/${deployment}-internal.git")
                            branch(repo_branch)
                                if (gitCredentialId) {
                                    credentials(gitCredentialId)
                                }
                        }
                        extensions {
                            cleanAfterCheckout()
                            pruneBranches()
                            relativeTargetDirectory('configuration-internal')
                        }
                    }
                }

                properties {
                    githubProjectUrl("https://github.com/edx/${deployment}-internal/")
                }

                triggers merge_to_master_trigger(repo_branch)

                steps{
                    configuration.environments.each { environment, environmentConfiguration ->
                        downstreamParameterized {
                            trigger("bastion-access-${environment}-${deployment}")
                        }
                    }
                }
            }

        }
    }
}


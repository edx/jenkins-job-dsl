/*
 
 Variables without defaults are marked (required) 
 
 Variables consumed for this job:
    * SECURE_GIT_CREDENTIALS: secure-bot-user (required)
    * NOTIFY_ON_FAILURE: alert@example.com
    * FOLER_NAME: folder, default is User-Mananagement
    * DEPLOYMENTS: (required)
        environments:
          - environment (required)
    * ACCESS_CONTROL: list of users to give access to
        - user
    * USER: user to run ansible (required)
    * DEPLOYMENT_KEY: ssh key that should be defined on the folder (required)
 
 This job expects the following credentials to be defined on the folder
    tools-edx-jenkins-aws-credentials: file with key/secret in boto config format
    find-active-instances-${deployment}-role-arn: the role to aws sts assume-role

*/


package devops.jobs
import static org.edx.jenkins.dsl.Constants.common_wrappers
import static org.edx.jenkins.dsl.Constants.common_logrotator
import static org.edx.jenkins.dsl.DevopsConstants.common_read_permissions

class AppPermissions{
    public static def job = { dslFactory, extraVars ->
        assert extraVars.containsKey('DEPLOYMENTS') : "Please define DEPLOYMENTS. It should be a list of strings."
        assert !(extraVars.get('DEPLOYMENTS') instanceof String) : "Make sure DEPLOYMENTS is a list and not a string"
        extraVars.get('DEPLOYMENTS').each { deployment, configuration ->
            configuration.environments.each { environment ->

                dslFactory.job(extraVars.get("FOLDER_NAME","User-Management") + "/app-permissions-${environment}-${deployment}") {

                    wrappers common_wrappers
                    logRotator common_logrotator

                    wrappers {
                        credentialsBinding {
                            file('AWS_CONFIG_FILE','tools-edx-jenkins-aws-credentials')
                            string('ROLE_ARN', "find-active-instances-${deployment}-role-arn")
                        }
                        assert extraVars.containsKey('DEPLOYMENT_KEY_NAME') : "Make sure you define the deployment key name"
                        sshAgent(extraVars.get('DEPLOYMENT_KEY_NAME'))
                      }

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

                    properties {
                        githubProjectUrl("https://github.com/edx/app-permissions/")
                    }

                    triggers {
                        githubPush()
                    }


                    def gitCredentialId = extraVars.get('SECURE_GIT_CREDENTIALS','')
                    
                    // The urls for the repos as well as the branch names have to be hardcoded in order for webhooks to work. 
                    // If they are parameterized, they are not defined until run time so the webhook cannot find them.
                    // To make the job more configurable, you can add back in parameters for repos and branches, but you have to change the trigger back to polling.
                    multiscm{
                        git {
                            remote {
                                url('https://github.com/edx/configuration.git')
                                branch('master')
                            }
                            extensions {
                                cleanAfterCheckout()
                                pruneBranches()
                                relativeTargetDirectory('configuration')
                            }
                        } 

                        git {
                            remote {
                                url('git@github.com:edx/app-permissions.git')
                                branch('master')
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

                    environmentVariables {
                        env('ENVIRONMENT', environment)
                        env('DEPLOYMENT', deployment)
                        env('USER', extraVars.get('USER', 'ubuntu'))
                    }
                    steps {
                        virtualenv {
                            nature("shell")
                            systemSitePackages(false)

                            command(
                                dslFactory.readFileFromWorkspace("devops/resources/run-app-permissions.sh")
                            )

                        }

                    }

                    if (extraVars.get('NOTIFY_ON_FAILURE')){
                        publishers {
                            mailer(extraVars.get('NOTIFY_ON_FAILURE'), false, false)
                        }
                    }

                }
            }
        }
    }

}
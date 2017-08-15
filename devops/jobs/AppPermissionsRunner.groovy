/*

 This job runs the script with the ansible to update app permissions. It is triggered by the app-permissions-watcher job.
 It consumes the same variables as the app-permissions-watcher job.
    
 Additionally, this job expects the following credentials to be defined on the folder
    tools-edx-jenkins-aws-credentials: file with key/secret in boto config format
    find-active-instances-${deployment}-role-arn: the role to aws sts assume-role

*/
package devops.jobs
import static org.edx.jenkins.dsl.Constants.common_wrappers
import static org.edx.jenkins.dsl.Constants.common_logrotator
import static org.edx.jenkins.dsl.DevopsConstants.common_read_permissions

class AppPermissionsRunner {
    public static def job = { dslFactory, extraVars ->
        assert extraVars.containsKey('DEPLOYMENTS') : "Please define DEPLOYMENTS. It should be a list of strings."
        assert !(extraVars.get('DEPLOYMENTS') instanceof String) : "Make sure DEPLOYMENTS is a list and not a string"
        extraVars.get('DEPLOYMENTS').each { deployment, configuration ->
            configuration.environments.each { environment ->
                dslFactory.job(extraVars.get("FOLDER_NAME","User-Management") + "/app-permissions-runner-${environment}-${deployment}") {

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

                    def access_control = extraVars.get('ACCESS_CONTROL', [])
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

                    assert extraVars.containsKey('APP_PERMISSIONS_REPO') : 'Make sure you define the app permissions repository'

                    def gitCredentialId = extraVars.get('SECURE_GIT_CREDENTIALS','')

                    parameters{
                        stringParam('CONFIGURATION_REPO', extraVars.get('CONFIGURATION_REPO', 'https://github.com/edx/configuration.git'),
                                'Git repo containing edX configuration.')
                        stringParam('CONFIGURATION_BRANCH', extraVars.get('CONFIGURATION_BRANCH', 'master'),
                                'e.g. tagname or origin/branchname')
                        stringParam('APP_PERMISSIONS_REPO', extraVars.get('APP_PERMISSIONS_REPO'),
                                'Git repo containing edx app permissions')
                        stringParam('APP_PERMISSIONS_BRANCH', extraVars.get('APP_PERMISSIONS_BRANCH', 'master'),
                                'e.g. tagname or origin/branchname')
                    }
                    
                    multiscm{ 
                        git {
                            remote {
                                url('$CONFIGURATION_REPO')
                                branch('$CONFIGURATION_BRANCH')
                            }
                            extensions {
                                cleanAfterCheckout()
                                pruneBranches()
                                relativeTargetDirectory('configuration')
                            }
                        }
                        git {
                            remote {
                                url('$APP_PERMISSIONS_REPO')
                                branch('$APP_PERMISSIONS_BRANCH')
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

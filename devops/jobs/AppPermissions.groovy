/*
 
 This job expects the following credentials to be defined on the folder
    tools-edx-jenkins-aws-credentials: file with key/secret in boto config format
    find-active-instances-${deployment}-role-arn: the role to aws sts assume-role
    ubuntu_deployment_201407: ssh key

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

                dslFactory.job(extraVars.get("FOLDER_NAME","App-Permissions") + "/app-permissions-${environment}-${deployment}") {

                    wrappers common_wrappers
                    logRotator common_logrotator

                    wrappers {
                        credentialsBinding {
                            file('AWS_CONFIG_FILE','tools-edx-jenkins-aws-credentials')
                            def role = "find-active-instances-${deployment}-role-arn"
                            string('ROLE_ARN', role)
                        }
                        def ssh_key = "ubuntu_deployment_201407"
                        sshAgent(ssh_key)
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

                    triggers {
                        cron("H/5 * * * *")
                    }


                    def gitCredentialId = extraVars.get('SECURE_GIT_CREDENTIALS','')
                    
                    parameters{
                        stringParam('CONFIGURATION_REPO', extraVars.get('CONFIGURATION_REPO', 'https://github.com/edx/configuration.git'),
                                        'Git repo containing edX configuration.')
                        stringParam('CONFIGURATION_BRANCH', extraVars.get('CONFIGURATION_BRANCH', 'master'),
                                'e.g. tagname or origin/branchname')

                        stringParam('APP_PERMISSIONS_REPO', extraVars.get('APP_PERMISSIONS_REPO', 'git@github.com:edx/app-permissions.git'),
                                            'Git repo containing app permissions.')
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

                    publishers {
                        mailer(extraVars.get('NOTIFY_ON_FAILURE',''), false, false)
                    }


                }
            }
        }
    }

}
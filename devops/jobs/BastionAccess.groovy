/*
 Variables consumed for this job:
      FOLER_NAME: folder, default is bastion_access
      DEPLOYMENTS: 
        environments:
          environment 
            bastion_host: DNS address of bastion host 
            ssh_access_credentials: SSH access key of bastion host
      SECURE_GIT_CREDENTIALS: secure-bot-user 
      NOTIFY_ON_FAILURE: alert@example.com

    This job expects the following credentials to be defined on the folder
    ubuntu-deployment-201407:   ubuntu deployment ssh access key
    ubuntu-mckinsey-deployment: mckinsey deployment ssh access key
*/

package devops.jobs
import static org.edx.jenkins.dsl.Constants.common_logrotator
import static org.edx.jenkins.dsl.Constants.common_wrappers


class BastionAccess {

    public static def job = { dslFactory, extraVars ->
        assert extraVars.containsKey('DEPLOYMENTS') : "Please define DEPLOYMENTS. It should be a list of strings."
        assert !(extraVars.get('DEPLOYMENTS') instanceof String) : "Make sure DEPLOYMENTS is a list and not a string"
        extraVars.get('DEPLOYMENTS').each { deployment, configuration ->
            configuration.environments.each { environment, bastion_config ->
                dslFactory.job(extraVars.get("FOLDER_NAME","bastion_access") + "/bastion-access-${environment}-${deployment}") {
                       
                    logRotator common_logrotator
                    wrappers common_wrappers
                    wrappers {
                        sshAgent(bastion_config.get('ssh_access_credentials'))
                    }

                    
                    def gitCredentialId = extraVars.get('SECURE_GIT_CREDENTIALS','')

                    parameters{
                        stringParam('CONFIGURATION_REPO', extraVars.get('CONFIGURATION_REPO', 'https://github.com/edx/configuration.git'),
                                    'Git repo containing edX configuration.')
                        stringParam('CONFIGURATION_BRANCH', extraVars.get('CONFIGURATION_BRANCH', 'master'),
                                'e.g. tagname or origin/branchname')
                        stringParam('CONFIGURATION_INTERNAL_REPO', extraVars.get('CONFIGURATION_INTERNAL_REPO',"git@github.com:edx/${deployment}-internal.git"),
                                'Secure Git repo .')
                        stringParam('CONFIGURATION_INTERNAL_BRANCH', extraVars.get('CONFIGURATION_INTERNAL_BRANCH', 'master'),
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
                                url('$CONFIGURATION_INTERNAL_REPO')
                                branch('$CONFIGURATION_INTERNAL_BRANCH')
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

                    // Check github every 30 minutes
                    triggers {
                        scm('H/30 * * * *')
                    }

                    environmentVariables {
                        env('ENVIRONMENT', environment)
                        env('DEPLOYMENT', deployment)
                        env('BASTION_HOST', bastion_config.get('bastion_host'))
                        env('USERS_YAML', bastion_config.get('bastion_user_yaml'))
                    }

                    steps {
                        virtualenv {
                            pythonName('System-CPython-3.6')
                            nature("shell")
                            systemSitePackages(false)

                            command(
                                dslFactory.readFileFromWorkspace("devops/resources/bastion-access.sh")
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

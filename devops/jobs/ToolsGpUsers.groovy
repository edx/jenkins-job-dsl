/*

 Variables without defaults are marked (required) 
 
 Variables consumed for this job:
    * SECURE_GIT_CREDENTIALS: secure-bot-user (required)
    * NOTIFY_ON_FAILURE: alert@example.com
    * FOLER_NAME: folder, default is User-Mananagement
    * ACCESS_CONTROL: list of users to give access to
        - user
    * CONFIGURATION_SECURE_REPO: repository where ansible vars are located (required)
 
 This job expects the following credentials to be defined on the folder
    ubuntu_deployment_201407: ssh key

*/

package devops.jobs
import static org.edx.jenkins.dsl.Constants.common_logrotator
import static org.edx.jenkins.dsl.Constants.common_wrappers
import static org.edx.jenkins.dsl.DevopsConstants.common_read_permissions


class ToolsGpUsers {

    public static def job = { dslFactory, extraVars ->
        dslFactory.job(extraVars.get("FOLDER_NAME","User-Management") + "/tools-gp-users") {
               
            logRotator common_logrotator
            wrappers common_wrappers

            wrappers{
                sshAgent("ubuntu_deployment_201407")
            }

            def access_control = extraVars.get('ACCESS_CONTROL',[])
            access_control.each { acl ->
                common_read_permissions.each { perm ->
                    authorization {
                        permission(perm, acl)
                    }
                }
                authorization{
                    permission('hudson.model.Item.Workspace', acl)
                }
                
            }

            assert extraVars.containsKey('CONFIGURATION_SECURE_REPO') : "Please define a configuration secure repo"


            def gitCredentialId = extraVars.get('SECURE_GIT_CREDENTIALS','')

            parameters{
                stringParam('CONFIGURATION_REPO', extraVars.get('CONFIGURATION_REPO', 'https://github.com/edx/configuration.git'),
                            'Git repo containing edX configuration.')
                stringParam('CONFIGURATION_BRANCH', extraVars.get('CONFIGURATION_BRANCH', 'master'),
                        'e.g. tagname or origin/branchname')
                stringParam('CONFIGURATION_SECURE_REPO', extraVars.get('CONFIGURATION_SECURE_REPO'),
                        'Git repo containing secure configuration')
                stringParam('CONFIGURATION_SECURE_BRANCH', extraVars.get('CONFIGURATION_SECURE_BRANCH', 'master'),
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
                        url('$CONFIGURATION_SECURE_REPO')
                        branch('$CONFIGURATION_SECURE_BRANCH')
                        if (gitCredentialId) {
                            credentials(gitCredentialId)
                        }
                    }
                    extensions {
                        cleanAfterCheckout()
                        pruneBranches()
                        relativeTargetDirectory('configuration-secure')
                    }
                }
            }

            steps {
                virtualenv {
                    nature("shell")
                    systemSitePackages(false)

                    command(
                        dslFactory.readFileFromWorkspace("devops/resources/tools-gp-users.sh")
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

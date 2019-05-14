package devops.jobs
import static org.edx.jenkins.dsl.Constants.common_wrappers
import static org.edx.jenkins.dsl.Constants.common_logrotator
import static org.edx.jenkins.dsl.DevopsConstants.common_read_permissions

class AppPermissionsFailure {
    public static def job = { dslFactory, extraVars ->
       dslFactory.job(extraVars.get("FOLDER_NAME","App-Permissions") + "/app-permissions-failure") {

                    wrappers common_wrappers
                    logRotator common_logrotator

                    wrappers {
                        credentialsBinding {
                            string('GIT_TOKEN',"edx_git_bot_token")
                        }
                    }

                    parameters {
                        stringParam('ENVIRONMENT')
                        stringParam('DEPLOYMENT')
                        stringParam('GIT_PREVIOUS_COMMIT_1')
                        stringParam('GIT_COMMIT_1')
                    }

                    steps {
                        virtualenv {
                            nature("shell")
                            systemSitePackages(false)

                            command(
                                dslFactory.readFileFromWorkspace("devops/resources/app-permission-runner-failure.sh")
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

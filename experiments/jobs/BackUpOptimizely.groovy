/*
Pulls current state of experiment code in Optimizely and backs up to git history branch in edx/optimizely-experiments.

Vars consumed for this job:
    * FOLDER_NAME: folder
    * NOTIFY_ON_FAILURE: alert@example.com
    * OPTIMIZELY_TOKEN: optimizely api token (required)
    * SECURE_GIT_CREDENTIALS: secure-bot-user (required)
    * SSH_AGENT_KEY : ssh-credential-name
*/
package experiments.jobs

class BackUpOptimizely {

    public static def job = { dslFactory, extraVars ->

                dslFactory.job(extraVars.get("FOLDER_NAME","Experiments") + "/back-up-optimizely") {

                    environmentVariables {
                        env('GIT_AUTHOR_NAME', extraVars.get('GIT_AUTHOR_NAME'))
                        env('GIT_AUTHOR_EMAIL', extraVars.get('GIT_AUTHOR_EMAIL'))
                        env('GIT_COMMITTER_NAME', extraVars.get('GIT_AUTHOR_NAME'))
                        env('GIT_COMMITTER_EMAIL', extraVars.get('GIT_AUTHOR_EMAIL'))
                    }

                    def gitCredentialId = extraVars.get('SECURE_GIT_CREDENTIALS','')

                    wrappers {
                        credentialsBinding {
                            string('OPTIMIZELY_TOKEN', 'optimizely-token')
                        }
                        sshAgent(gitCredentialId)
                      }

                    multiscm {
                        git {
                            remote {
                                url(extraVars.get('OPTIMIZELY_EXPERIMENTS_REPO'))
                                branch("history")
                                if (gitCredentialId) {
                                    credentials(gitCredentialId)
                                }
                            }
                            extensions {
                                cleanAfterCheckout()
                                pruneBranches()
                                relativeTargetDirectory('optimizely-experiments')
                            }
                        }
                        git {
                            remote {
                                url("https://github.com/edx/py-opt-cli.git")
                                branch('master')
                            }
                            extensions {
                                cleanAfterCheckout()
                                pruneBranches()
                                relativeTargetDirectory('py-opt-cli')
                            }
                        }
                    }


                    triggers {
                        // Trigger twice a day
                        cron("H 0,12 * * *")
                    }

                    steps {
                        virtualenv {
                            pythonName('System-CPython-3.6')
                            nature("shell")
                            systemSitePackages(false)

                            command(
                                dslFactory.readFileFromWorkspace("experiments/resources/back_up_optimizely.sh")
                           )

                        }
                    }

                // Not gonna notify on failures until we solve the clean commit issue.
                //publishers {
                //        mailer(extraVars.get('NOTIFY_ON_FAILURE'), false, false)
                //}
        }
    }

}

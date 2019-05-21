/*
    Variables without defaults are marked (required)

    Variables consumed for this job:
        * CONFIGURATION_SECURE_BRANCH: origin/master
        * NOTIFY_ON_FAILURE: alert@example.com
        * FOLDER_NAME: folder
        * SECURE_GIT_CREDENTIALS: secure-bot-user (required)
        * ASG_LIFECYCLE_HOOKS_SNITCH:

    This job expects the following credentials to be defined on the folder
    tools-edx-jenkins-aws-credentials: file with key/secret in boto config format
    asg-lifecycle-hooks-monitoring-role-${deployment}-arn: the role to aws sts assume-role


*/
package devops.jobs
import static org.edx.jenkins.dsl.Constants.common_wrappers
import static org.edx.jenkins.dsl.Constants.common_logrotator
import static org.edx.jenkins.dsl.DevopsTasks.common_parameters
import static org.edx.jenkins.dsl.DevopsTasks.common_multiscm
import org.yaml.snakeyaml.error.YAMLException

class CheckASGLifeCycleHooks {
    public static def job = { dslFactory, extraVars ->
        assert extraVars.containsKey('DEPLOYMENTS') : "Please define DEPLOYMENTS. It should be a list of strings."
        assert !(extraVars.get('DEPLOYMENTS') instanceof String) : "Make sure DEPLOYMENTS is a list and not a string"
        extraVars.get('DEPLOYMENTS').each { deployment, configuration ->
            configuration.asgs.each { asg ->

                dslFactory.job(extraVars.get("FOLDER_NAME","Monitoring") + "/check-asg-lifecycle-hooks-${deployment}") {
                    wrappers common_wrappers
                    /* Only keep the builds for one day since it runs every thirty minutes.
                    */
                    parameters {
                        stringParam('SYSADMIN_REPO', 'git@github.com:edx-ops/sysadmin.git')
                        stringParam('SYSADMIN_BRANCH', 'master')
                    }

                    logRotator {
                        daysToKeep(1)
                    }

                    def config_internal_repo = "git@github.com:edx/${deployment}-internal.git"
                    def config_secure_repo = "git@github.com:edx-ops/${deployment}-secure.git" 

                    extraVars['CONFIGURATION_INTERNAL_REPO'] = config_internal_repo
                    extraVars['CONFIGURATION_SECURE_REPO'] = config_secure_repo

                    triggers {
                        cron("H/30 * * * *")
                    }

                    parameters common_parameters(extraVars)

                    multiscm common_multiscm(extraVars)

                    multiscm {
                        git {
                            remote {
                                url('$SYSADMIN_REPO')
                                branch('$SYSADMIN_BRANCH')
                                credentials(extraVars.get('SECURE_GIT_CREDENTIALS'))
                            }
                            extensions {
                                cleanAfterCheckout()
                                pruneBranches()
                                relativeTargetDirectory('configuration')
                            }
                        }
                    }

                    def snitch = configuration['snitch']

                    environmentVariables {
                        env('REGION', extraVars.get('REGION'))
                        env('DEPLOYMENT', deployment)
                        env('ASG', asg)
                    }

                    steps {
                        virtualenv {
                            pythonName('System-CPython-2.7')
                            nature("shell")
                            systemSitePackages(false)
                            command(
                                dslFactory.readFileFromWorkspace("devops/resources/check-lifecycle-hooks.sh")
                            )
                        }

                        if (snitch) {
                            shell("curl $snitch")
                        }
                    }
                }
            }
        }
    }
}

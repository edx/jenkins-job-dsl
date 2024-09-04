/*
    Variables without defaults are marked (required)

    Variables consumed for this job:
        * CONFIGURATION_SECURE_BRANCH: origin/master
        * NOTIFY_ON_FAILURE: alert@example.com
        * FOLDER_NAME: folder
        * SECURE_GIT_CREDENTIALS: secure-bot-user (required)

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
                        stringParam('CONFIGURATION_REPO', 'https://github.com/edx/configuration.git')
                        stringParam('CONFIGURATION_BRANCH', 'master')
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

                    wrappers {
                        credentialsBinding {
                            string('ROLE_ARN', "check-lifecycle-hooks-${deployment}-role-arn")
                            string("GENIE_KEY", "opsgenie_heartbeat_key")
                            string("DD_KEY", "datadog_heartbeat_key")
                        }
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
                                relativeTargetDirectory('sysadmin')
                            }
                        }
                        git {
                            remote {
                                url('$CONFIGURATION_REPO')
                                branch('$CONFIGURATION_BRANCH')
                                credentials(extraVars.get('SECURE_GIT_CREDENTIALS'))
                            }
                            extensions {
                                cleanAfterCheckout()
                                pruneBranches()
                                relativeTargetDirectory('configuration')
                            }
                        }
                    }

                    environmentVariables {
                        env('REGION', extraVars.get('REGION'))
                        env('DEPLOYMENT', deployment)
                        env('ASG', asg)
                    }

                    steps {
                       shell(dslFactory.readFileFromWorkspace('devops/resources/check-lifecycle-hooks.sh'))


                        String opsgenie_heartbeat_name = configuration.get('opsgenie_heartbeat_name','')
                        if (opsgenie_heartbeat_name) {
                             shell('curl -X GET "https://api.opsgenie.com/v2/heartbeats/'+opsgenie_heartbeat_name+'/ping" -H "Authorization: GenieKey ${GENIE_KEY}"')
                        }
                        String datadog_heartbeat_name = configuration.get('DATADOG_HEARTBEAT_NAME', '')
                        if (datadog_heartbeat_name) {
                            String DD_JSON = """
                                {
                                    "series": [{
                                        "metric": "${datadog_heartbeat_name}",
                                        "points": [['\$(date +%s)', 1]],
                                        "type": "gauge",
                                        "tags": ["deployment:${deployment}"]
                                    }]
                                }
                                """

                                shell('curl -X POST "https://api.datadoghq.com/api/v1/series?api_key=${DD_KEY}" -H "Content-Type: application/json" -d \'' + DD_JSON + '\'')
                        }
                    }
                }
            }
        }
    }
}

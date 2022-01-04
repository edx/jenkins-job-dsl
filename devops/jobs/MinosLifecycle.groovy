/*

 This job creates two jobs:
 - retire-instances-in-terminating-wait
 - terminate-instances-that-have-been-verified-for-retirement

 Variables without defaults are marked (required) 
 
 Variables consumed for this job:
    * DEPLOYMENTS: (required)
        deployment:
          environments:
            environment (required)
                opsgenie_heartbeat_name: opsgenie heartbeat name (required)
    * SSH_ACCESS_CREDENTIALS: ssh access credentials, should be defined on the folder (required)
    * CONFIGURATION_REPO: name of config repo, default is https://github.com/edx/configuration.git
    * CONFIGURATION_BRANCH: default is master
    * NOTIFY_ON_FAILURE: alert@example.com
    * FOLER_NAME: folder, default is Monitoring
    * AWS_REGION: AWS region to use, default is us-east-1
 
 This job expects the following credentials to be defined on the folder
    tools-edx-jenkins-aws-credentials: file with key/secret in boto config format
    minos-lifecycle-${deployment}-role-arn: the role to aws sts assume-role



*/
package devops.jobs
import static org.edx.jenkins.dsl.Constants.common_wrappers
import static org.edx.jenkins.dsl.Constants.common_logrotator

class MinosLifecycle {
    public static def job = { dslFactory, extraVars ->
        assert extraVars.containsKey('DEPLOYMENTS') : "Please define DEPLOYMENTS. It should be a list of strings."
        assert !(extraVars.get('DEPLOYMENTS') instanceof String) : "Make sure DEPLOYMENTS is a list and not a string"
        extraVars.get('DEPLOYMENTS').each { deployment, configuration ->
            configuration.environments.each { environment, inner_config ->
                dslFactory.job(extraVars.get("FOLDER_NAME","Monitoring") + "/retire-instances-in-terminating-wait-${environment}-${deployment}") {

                    wrappers common_wrappers
                    
                    assert extraVars.containsKey('SSH_ACCESS_CREDENTIALS') : "Please define SSH_ACCESS_CREDENTIALS"
                   
                    wrappers {
                        credentialsBinding{
                            file('AWS_CONFIG_FILE','tools-edx-jenkins-aws-credentials')
                            string('ROLE_ARN', "minos-lifecycle-${deployment}-role-arn")
                            string("GENIE_KEY", "opsgenie_heartbeat_key")
                        } 
                        sshAgent(extraVars.get('SSH_ACCESS_CREDENTIALS')) 
                        timeout {
                            absolute(30)  // 15 minutes
                            failBuild()
                        }
                    }

                    logRotator common_logrotator

                    properties {
                        rebuild {
                            autoRebuild(false)
                            rebuildDisabled(false)
                        }
                    }

                    triggers {
                        cron('H/10 * * * *')
                    }

                    parameters{
                        stringParam('CONFIGURATION_REPO', extraVars.get('CONFIGURATION_REPO', 'https://github.com/edx/configuration.git'),
                                'Git repo containing edX configuration.')
                        stringParam('CONFIGURATION_BRANCH', extraVars.get('CONFIGURATION_BRANCH', 'master'),
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
                    }

                    environmentVariables {
                        env('ENVIRONMENT', environment)
                        env('DEPLOYMENT', deployment)
                        env('AWS_REGION',extraVars.get('AWS_REGION','us-east-1'))
                    }

                    steps {

                        virtualenv {
                            pythonName('System-CPython-3.6')
                            nature("shell")
                            systemSitePackages(false)
                            command(dslFactory.readFileFromWorkspace("devops/resources/retire-instances-in-terminating-wait.sh"))
                        }

                        
                        String opsgenie_heartbeat_name = inner_config.get('opsgenie_heartbeat_name', '')
                        if (opsgenie_heartbeat_name) {
                             shell('curl -X GET "https://api.opsgenie.com/v2/heartbeats/'+opsgenie_heartbeat_name+'/ping" -H "Authorization: GenieKey ${GENIE_KEY}"')
                        }

                        downstreamParameterized {
                            trigger("terminate-instances-that-have-been-verified-for-retirement-${environment}-${deployment}")
                        }
                    }

                    if (extraVars.get('NOTIFY_ON_FAILURE')){
                        publishers {
                            mailer(extraVars.get('NOTIFY_ON_FAILURE'), false, false)
                        }
                    }

                }

                dslFactory.job(extraVars.get("FOLDER_NAME","Monitoring") + "/terminate-instances-that-have-been-verified-for-retirement-${environment}-${deployment}") {

                    description("This job issue lifecycle proceed AWS api commands to servers that have been tagged as ready for termination by minos.")

                    wrappers common_wrappers
                    logRotator common_logrotator

                    wrappers { 
                        credentialsBinding{
                            file('AWS_CONFIG_FILE','tools-edx-jenkins-aws-credentials')
                            string('ROLE_ARN', "minos-lifecycle-${deployment}-role-arn")
                        } 
                        timeout {
                            absolute(15)  // 15 minutes
                            failBuild()
                        }
                    }
                    
                    properties {
                        rebuild {
                            autoRebuild(false)
                            rebuildDisabled(false)
                        }
                    }


                    triggers {
                        cron('H/5 * * * *')
                    }

                    parameters{
                        stringParam('CONFIGURATION_REPO', extraVars.get('CONFIGURATION_REPO', 'https://github.com/edx/configuration.git'),
                                'Git repo containing edX configuration.')
                        stringParam('CONFIGURATION_BRANCH', extraVars.get('CONFIGURATION_BRANCH', 'master'),
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
                    }

                    environmentVariables {
                        env('ENVIRONMENT', environment)
                        env('DEPLOYMENT', deployment)
                        env('AWS_REGION', extraVars.get('AWS_REGION','us-east-1'))
                    }

                    steps {
                       shell(dslFactory.readFileFromWorkspace('devops/resources/terminate-instances-that-have-been-verified-for-retirement.sh'))
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


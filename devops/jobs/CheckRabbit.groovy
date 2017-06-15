/*

    Variables consumed from the EXTRA_VARS input to your seed job in addition
    to those listed in createMonitoringJobs.

    * SSH_USER: monitor
    * RABBIT_SNITCH

    This job expects the following credentials to be defined on the folder
    tools-edx-jenkins-aws-credentials: file with key/secret in boto config format
    ec2py-role-${deployment}_arn: the role to aws sts assume-role
    rabbit_monitoring_${environment}_${deployment}_ssh_key
   

*/
package devops.jobs

import static org.edx.jenkins.dsl.Constants.common_wrappers
import static org.edx.jenkins.dsl.Constants.common_logrotator
import static org.edx.jenkins.dsl.DevopsTasks.common_parameters
import static org.edx.jenkins.dsl.DevopsTasks.common_multiscm

class CheckRabbit {
    public static def job = { dslFactory, extraVars ->
        assert extraVars.containsKey('DEPLOYMENTS') : "Please define DEPLOYMENTS. It should be a list of strings."
        assert !(extraVars.get('DEPLOYMENTS') instanceof String) : "Make sure DEPLOYMENTS is a list and not a string"
        extraVars.get('DEPLOYMENTS').each { deployment, configuration ->
            configuration.environments.each { environment ->

                dslFactory.job(extraVars.get("FOLDER_NAME","Monitoring") + "/check-rabbitmq-${environment}-${deployment}") {

                    wrappers common_wrappers
                    logRotator common_logrotator

                    wrappers {
                        credentialsBinding {
                            file('AWS_CONFIG_FILE','tools-edx-jenkins-aws-credentials')
                            def variable = "ec2py-role-${deployment}-arn"
                            string('ROLE_ARN', variable)
                        }
                        def ssh_key = "rabbit_monitoring_${environment}_${deployment}_ssh_key"
                        sshAgent(ssh_key)
                      }

                    def config_internal_repo = "git@github.com:edx/${deployment}-internal.git"
                    def config_secure_repo = "git@github.com:edx-ops/${deployment}-secure.git" 

                    extraVars['CONFIGURATION_INTERNAL_REPO'] = config_internal_repo
                    extraVars['CONFIGURATION_SECURE_REPO'] = config_secure_repo
                    
                    def cluster_name = 'rabbit'
                    if (configuration.get('cluster_name')){
                        cluster_name = configuration.get('cluster_name')
                    }

                    def sudo_user = 'ubuntu'
                    if (configuration.get('sudo_user')){
                        sudo_user = configuration.get('sudo_user')
                    }
                    
                    if (environment == 'prod'){
                        extraVars['NOTIFY_ON_FAILURE'] = 'tools-edx-jenkins-alert@edx.opsgenie.net'
                    }
                    else{
                        extraVars['NOTIFY_ON_FAILURE'] = 'devops+non-critical@edx.org'
                    }

                    properties {
                        rebuild {
                            autoRebuild(false)
                            rebuildDisabled(false)
                        }
                    }

                    throttleConcurrentBuilds {
                        maxPerNode(0)
                        maxTotal(0)
                    }

                    triggers {
                        cron("H/10 * * * *")
                    }

                    parameters common_parameters(extraVars)

                    multiscm common_multiscm(extraVars)

                    environmentVariables {
                        env('SSH_USER', extraVars.get('SSH_USER','monitor'))
                        env('ENVIRONMENT', environment)
                        env('DEPLOYMENT', deployment)
                        env('CLUSTER_NAME', cluster_name)
                        env('SUDO_USER', sudo_user)
                    }

                    steps {
                        virtualenv {
                            nature("shell")
                            systemSitePackages(false)

                            command(
                                dslFactory.readFileFromWorkspace("devops/resources/check-rabbit.sh")
                            )

                        }

                        String snitch =  extraVars.get('RABBIT_SNITCH','')
                        if (snitch) {
                            shell("curl $snitch")
                        }
                    }

                    publishers {
                        mailer(extraVars.get('NOTIFY_ON_FAILURE','devops+non-critical@edx.org'), false, false)
                    }


                }
            }
        }
    }

}

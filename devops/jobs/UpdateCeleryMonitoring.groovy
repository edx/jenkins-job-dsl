/*
    Variables without defaults are marked (required)

    Variables consumed for this job:
        * CONFIGURATION_SECURE_BRANCH: origin/master
        * DEPLOYMENTS: (required)
            aws_region: (required)
            environments:
              environment:
                redis_host: (required)
                sns_topic: (required)
                thresholds:
                  queue_name: 1000
        * NOTIFY_ON_FAILURE: alert@example.com
        * FOLDER_NAME: folder
        * SECURE_GIT_CREDENTIALS: secure-bot-user (required)
        * REDIS_SNITCH:

    This job expects the following credentials to be defined on the folder
    tools-edx-jenkins-aws-credentials: file with key/secret in boto config format
    redis-monitoring-role-${deployment}-arn: the role to aws sts assume-role


*/
package devops.jobs

import static org.edx.jenkins.dsl.Constants.common_wrappers
import static org.edx.jenkins.dsl.Constants.common_logrotator
import static org.edx.jenkins.dsl.DevopsTasks.common_parameters
import static org.edx.jenkins.dsl.DevopsTasks.common_multiscm

class UpdateCeleryMonitoring {
    public static def job = { dslFactory, extraVars ->
        assert extraVars.containsKey('DEPLOYMENTS') : "Please define DEPLOYMENTS. It should be a list of strings."
        assert !(extraVars.get('DEPLOYMENTS') instanceof String) : "Make sure DEPLOYMENTS is a list and not a string"
        extraVars.get('DEPLOYMENTS').each { deployment, configuration ->
            configuration.environments.each { environment, redis_config ->

                dslFactory.job(extraVars.get("FOLDER_NAME","Monitoring") + "/update-celery-monitoring-${environment}-${deployment}") {

                    wrappers common_wrappers
                    /* Only keep the builds for one day since it runs every minute.
                       Reduce number of builds kept from ~10,000 to ~1000
                    */
                    logRotator {
                        daysToKeep(1)
                    }

                    wrappers {
                        credentialsBinding {
                            file('AWS_CONFIG_FILE','tools-edx-jenkins-aws-credentials')
                            def variable = "redis-monitoring-${deployment}-role-arn"
                            string('ROLE_ARN', variable)
                        }
                    }

                    def config_internal_repo = "git@github.com:edx/${deployment}-internal.git"
                    def config_secure_repo = "git@github.com:edx-ops/${deployment}-secure.git" 

                    extraVars['CONFIGURATION_INTERNAL_REPO'] = config_internal_repo
                    extraVars['CONFIGURATION_SECURE_REPO'] = config_secure_repo

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
                        cron("* * * * *")
                    }

                    parameters common_parameters(extraVars)

                    multiscm common_multiscm(extraVars)

                    def thresholds = ""
                    redis_config.thresholds.each { queue, threshold ->
                        thresholds = "${thresholds}--queue-threshold ${queue} ${threshold} "
                    }

                    environmentVariables {
                        env('ENVIRONMENT', environment)
                        env('DEPLOYMENT', deployment)
                        env('REDIS_HOST', redis_config.get('redis_host'))
                        env('SNS_TOPIC', redis_config.get('sns_topic'))
                        env('AWS_DEFAULT_REGION', configuration.get('aws_region'))
                        env('THRESHOLDS', thresholds)
                    }

                    steps {
                       shell(dslFactory.readFileFromWorkspace('devops/resources/update_celery_monitoring.sh'))
                        String snitch =  extraVars.get('REDIS_SNITCH','')
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

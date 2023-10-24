/*

 Variables consumed for this job:
    * DEPLOYMENTS (required)
        - deployment
    * NOTIFY_ON_FAILURE: alert@example.com
    * FOLDER_NAME: folder, default is Monitoring

    This job expects the following credentials to be defined on the folder
    tools-edx-jenkins-aws-credentials: file with key/secret in boto config format
    create-asg-notifications-${deployment}-role-arn: the role to aws sts assume-role
    asg-notifications-sns-topic-arn-${deployment}: SNS topic arn to notify for asg instance launch errors 
*/

package devops.jobs
import static org.edx.jenkins.dsl.Constants.common_logrotator
import static org.edx.jenkins.dsl.Constants.common_wrappers

class CreateASGNotifications {

    public static def job = { dslFactory, extraVars ->
        extraVars.get('DEPLOYMENTS').each { deployment ->
                dslFactory.job(extraVars.get("FOLDER_NAME","MONITORING") + "/create-asg-notifications-${deployment}") {
                       
                    logRotator common_logrotator
                    wrappers common_wrappers
                    
                    wrappers {
                        credentialsBinding {
                            string("ROLE_ARN","create-asg-notifications-${deployment}-role-arn")
                            string("SNS_TOPIC_ARN","asg-notifications-${deployment}-sns-topic-arn")
                        }
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

                    triggers {
                        cron('H * * * *')
                    }

                    steps {
                        shell(dslFactory.readFileFromWorkspace('devops/resources/create-asg-notifications.sh'))

                    }

                    if (extraVars.get('NOTIFY_ON_FAILURE')){
                        publishers {
                            extendedEmail {
                                recipientList(extraVars.get('NOTIFY_ON_FAILURE'))
                                triggers {
                                     failure {
                                         attachBuildLog(false)  // build log contains PII!
                                         compressBuildLog(false)  // build log contains PII!
                                         subject('Failed build: ${JOB_NAME} #${BUILD_NUMBER}')
                                         content('Jenkins job: ${JOB_NAME} failed. \nFor' + " ${deployment} " + 'Environment. \n\nSee ${BUILD_URL} for details.')
                                         contentType('text/plain')
                                         sendTo {
                                             recipientList()
                                         }
                                     }
                                }
                            }
                        }
                    }
                }
          }
    }
}

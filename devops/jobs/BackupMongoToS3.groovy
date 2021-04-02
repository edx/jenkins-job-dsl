/*
 Backup mongo to s3 bucket

 Variables without defaults are marked (required) 
 
 Variables consumed for this job:
    * SECURE_GIT_CREDENTIALS: secure-bot-user (required)
    * NOTIFY_ON_FAILURE: alert@example.com
    * FOLER_NAME: folder
    * SYS_ADMIN_REPO: repo where the mongo backup script is located (required)
    * STATUS_BRANCH: default is master
    * DEPLOYMENTS: (required)
        environments:
          - environment (required)
        ip_addresses: IP addresses of the database hosts (required)
        snitch: deadmans snitch

    This job expects the following credentials to be defined on the folder
    tools-edx-jenkins-aws-credentials: file with key/secret in boto config format
    mongohq-backups-${environment}-${deployment}-role-arn: the role to aws sts assume-role
    mongo-db-password: the password for the mongo databases

*/

package devops.jobs
import static org.edx.jenkins.dsl.Constants.common_logrotator
import static org.edx.jenkins.dsl.Constants.common_wrappers


class BackupMongoToS3 {

    public static def job = { dslFactory, extraVars ->
        assert extraVars.containsKey('DEPLOYMENTS') : "Please define DEPLOYMENTS. It should be a list of strings."
        assert !(extraVars.get('DEPLOYMENTS') instanceof String) : "Make sure DEPLOYMENTS is a list and not a string"
        extraVars.get('DEPLOYMENTS').each { deployment, configuration ->
            configuration.environments.each { environment ->
                dslFactory.job(extraVars.get("FOLDER_NAME","Monitoring") + "/backup-${environment}-${deployment}-mongo-to-s3") {
                       
                    logRotator common_logrotator
                    wrappers common_wrappers

                    wrappers{
                        credentialsBinding{
                            file('AWS_CONFIG_FILE','tools-edx-jenkins-aws-credentials')
                            string('ROLE_ARN', "mongohq-backups-${environment}-${deployment}-role-arn")
                            string('MONGO_DB_PASSWORD', 'mongo-db-password')
                            string("GENIE_KEY", "opsgenie_heartbeat_key")
                        }
                    }

                    assert extraVars.containsKey('SYSADMIN_REPO') : "Please define a system admin repo where the mongo backup script is located"


                    def gitCredentialId = extraVars.get('SECURE_GIT_CREDENTIALS','')

                    parameters{
                        stringParam('CONFIGURATION_REPO', extraVars.get('CONFIGURATION_REPO', 'https://github.com/edx/configuration.git'),
                                    'Git repo containing edX configuration.')
                        stringParam('CONFIGURATION_BRANCH', extraVars.get('CONFIGURATION_BRANCH', 'master'),
                                'e.g. tagname or origin/branchname')
                        stringParam('SYSADMIN_REPO', extraVars.get('SYSADMIN_REPO'),
                                'Git repo containing sysadmin configuration which contains the mongo backup script.')
                        stringParam('SYSADMIN_BRANCH', extraVars.get('SYSADMIN_BRANCH', 'master'),
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
                                url('$SYSADMIN_REPO')
                                branch('$SYSADMIN_BRANCH')
                                if (gitCredentialId) {
                                    credentials(gitCredentialId)
                                }
                            }
                            extensions {
                                cleanAfterCheckout()
                                pruneBranches()
                                relativeTargetDirectory('sysadmin')
                            }
                        }
                    }

                    triggers{
                        cron('0 H/12 * * *')
                    }

                    assert configuration.containsKey('ip_addresses') : "Please define IP addresses of the database hosts"

                    environmentVariables {
                        env('ENVIRONMENT', environment)
                        env('DEPLOYMENT', deployment)
                        env('IP_ADDRESSES', configuration.get('ip_addresses'))
                    }

                    steps {
                        shell(dslFactory.readFileFromWorkspace('devops/resources/backup-mongo-to-s3.sh'))

                        String opsgenie_heartbeat_name = configuration.get('opsgenie_heartbeat_name','')
                        if (opsgenie_heartbeat_name) {
                             shell('curl -X GET "https://api.opsgenie.com/v2/heartbeats/'+opsgenie_heartbeat_name+'/ping" -H "Authorization: GenieKey ${GENIE_KEY}"')
                        }
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
}

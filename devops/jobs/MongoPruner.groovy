/*
 
 Variables consumed for this job:
    * NOTIFY_ON_FAILURE: alert@example.com
    * FOLER_NAME: folder
    * TUBULAR_REPO: repo where the mongo pruner script is located (required)
    * TUBULAR_BRANCH: default is master
    * DEPLOYMENTS: (required)
        environments:
            environment (required)
              database_name: mongo database name (required) 
        mongo_user: mongo user to authenticate and access db's (required)
    This job expects the following credentials to be defined on the folder
    tools-edx-jenkins-aws-credentials: file with key/secret in boto config format
    mongo-pruner-${deployment}-role-arn: the role to aws sts assume-role
    mongo-db-password: the password for the mongo database

*/

package devops.jobs
import static org.edx.jenkins.dsl.Constants.common_logrotator
import static org.edx.jenkins.dsl.Constants.common_wrappers


class MongoPruner {

    public static def job = { dslFactory, extraVars ->
        assert extraVars.containsKey('DEPLOYMENTS') : "Please define DEPLOYMENTS. It should be a list of strings."
        assert !(extraVars.get('DEPLOYMENTS') instanceof String) : "Make sure DEPLOYMENTS is a list and not a string"
        extraVars.get('DEPLOYMENTS').each { deployment, configuration ->
            configuration.environments.each { environment, inner_config ->
                dslFactory.job(extraVars.get("FOLDER_NAME","Monitoring") + "/mongo-pruner-${environment}-${deployment}") {
                       
                    logRotator {
                        daysToKeep(30) // keep jobs around for 30 days.
                    }
                    wrappers common_wrappers

                    wrappers{
                        credentialsBinding{
                            string('MONGO_PASSWORD', "mongo-db-password-${environment}-${deployment}")
                            string('ROLE_ARN', "mongo-pruner-${deployment}-role-arn")
                        }
                    }

                    assert extraVars.containsKey('TUBULAR_REPO') : "Please define a tubular repo where the mongo pruner script is located"

                    parameters{
                        stringParam('TUBULAR_REPO', extraVars.get('TUBULAR_REPO'),
                                'Git repo which contains the mongo pruner script.')
                        stringParam('TUBULAR_BRANCH', extraVars.get('TUBULAR_BRANCH', 'master'),
                                'e.g. tagname or origin/branchname')
                    }

                    multiscm{
                        git {
                            remote {
                                url('$TUBULAR_REPO')
                                branch('$TUBULAR_BRANCH')
                            }
                            extensions {
                                cleanAfterCheckout()
                                pruneBranches()
                                relativeTargetDirectory('tubular')
                            }
                        }
                    }
                    
                    triggers{
                        cron('H 0 * * 0')
                    }

                    environmentVariables {
                        env('ENVIRONMENT', environment)
                        env('DEPLOYMENT', deployment)
                        env('DATABASE_NAME', inner_config.get('database_name'))
                        env('MONGO_USER', configuration.get('mongo_prune_user'))
                    }

                    steps {
                       shell(dslFactory.readFileFromWorkspace('devops/resources/mongo-pruner.sh'))
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

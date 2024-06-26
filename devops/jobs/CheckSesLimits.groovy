/*

    This job expects the following credentials to be defined on the folder
    tools-edx-jenkins-aws-credentials: file with key/secret in boto config format
    ses-role-${deployment}_arn: the role to aws sts assume-role

    Variables without defaults are marked (required)

    Variables consumed for this job:
        * NOTIFY_ON_FAILURE: (Required - email address)
        * SECURE_GIT_CREDENTIALS: (Required - jenkins name of git credentials)
        * CRIT_THRESHOLD: (Required - int or float between 0 and 100)
        * WARN_THRESHOLD: (Required - int or float between 0 and 100)
        * DEPLOYMENTS: (Required)
          deployment_name: (Required)
            regions: (Required - space separated list of aws regions)
            CONFIGURATION_INTERNAL_REPO: (Optional - git uri)

*/
package devops.jobs

import static org.edx.jenkins.dsl.Constants.common_wrappers
import static org.edx.jenkins.dsl.Constants.common_logrotator
import static org.edx.jenkins.dsl.DevopsTasks.common_parameters002
import static org.edx.jenkins.dsl.DevopsTasks.common_multiscm002

class CheckSesLimits {
    public static def job = { dslFactory, extraVars ->
        assert extraVars.containsKey('DEPLOYMENTS') : "Please define DEPLOYMENTS. It should be a list of strings."
        assert extraVars.containsKey('CRIT_THRESHOLD') : "Please define CRIT_THRESHOLD. It should be a percentage expressed as a float between 0 to 100."
        assert extraVars.containsKey('WARN_THRESHOLD') : "Please define WARN_THRESHOLD. It should be a percentage expressed as a float between 0 to 100."
        assert extraVars.containsKey('NOTIFY_ON_FAILURE') : "Please define NOTIFY_ON_FAILURE."
        assert !(extraVars.get('DEPLOYMENTS') instanceof String) : "Make sure DEPLOYMENTS is a list and not a string"
        assert (extraVars.get('CRIT_THRESHOLD') instanceof Number) : "Make sure CRIT_THRESHOLD is a number between 0 and 100"
        assert (extraVars.get('WARN_THRESHOLD') instanceof Number) : "Make sure WARN_THRESHOLD is a number between 0 and 100"
        extraVars.get('DEPLOYMENTS').each { deployment, configuration ->

            dslFactory.job(extraVars.get("FOLDER_NAME","Monitoring") + "/check-ses-limits-${deployment}") {

                wrappers common_wrappers
                logRotator common_logrotator

                wrappers {
                    credentialsBinding {
                        def variable = "ses-role-${deployment}-arn"
                        string('ROLE_ARN', variable)
                    }
                  }

                def crit_threshold = extraVars.get('CRIT_THRESHOLD')
                def warn_threshold = extraVars.get('WARN_THRESHOLD')
                def regions = configuration.get('regions')

                if (!configuration.containsKey('CONFIGURATION_INTERNAL_REPO')) {
                        extraVars['CONFIGURATION_INTERNAL_REPO'] = "git@github.com:edx/${deployment}-internal.git"
                }
                else {
                        extraVars['CONFIGURATION_INTERNAL_REPO'] = configuration['CONFIGURATION_INTERNAL_REPO']
                }

                throttleConcurrentBuilds {
                    maxPerNode(0)
                    maxTotal(0)
                }

                triggers {
                    cron("H/10 * * * *")
                }

                parameters common_parameters002(extraVars)

                multiscm common_multiscm002(extraVars)

                environmentVariables {
                    env('DEPLOYMENT', deployment)
                    env('CRIT_THRESHOLD', crit_threshold)
                    env('WARN_THRESHOLD', warn_threshold)
                    env('REGIONS', regions)
                }

                steps {
                  shell(dslFactory.readFileFromWorkspace('devops/resources/check-ses-limits.sh'))

                }

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

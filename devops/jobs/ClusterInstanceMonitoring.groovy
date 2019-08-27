/*
 Vars consumed for this job:
    * DEPLOYMENTS: (required)
        deployment:
          internal_repo: repo for internal configuration 

    * SECURE_GIT_CREDENTIALS: secure-bot-user (required)
    * NOTIFY_ON_FAILURE: alert@example.com
    * FOLDER_NAME: folder
 
 This job expects the following credentials to be defined on the folder
    tools-edx-jenkins-aws-credentials: file with key/secret in boto config format
    find-active-instances-${deployment}-role-arn: the role to aws sts assume-role

*/

package devops.jobs
import static org.edx.jenkins.dsl.Constants.common_wrappers
import static org.edx.jenkins.dsl.Constants.common_logrotator

class ClusterInstanceMonitoring{
    public static def job = { dslFactory, extraVars ->
        assert extraVars.containsKey('DEPLOYMENTS') : "Please define DEPLOYMENTS. It should be a list of strings."
        assert !(extraVars.get('DEPLOYMENTS') instanceof String) : "Make sure DEPLOYMENTS is a list and not a string"
        extraVars.get('DEPLOYMENTS').each { deployment, configuration ->
            
            dslFactory.job(extraVars.get("FOLDER_NAME","Monitoring") + "/cluster-instance-monitoring-${deployment}") {
                wrappers common_wrappers
                logRotator common_logrotator

                wrappers {
                    credentialsBinding {
                        file('AWS_CONFIG_FILE','tools-edx-jenkins-aws-credentials')
                        string('ROLE_ARN', "find-active-instances-${deployment}-role-arn")
                    }
                }

                def gitCredentialId = extraVars.get('SECURE_GIT_CREDENTIALS','')
                
                def internal_config = "git@github.com:edx/${deployment}-internal.git"
                if (configuration.get('internal_repo')){
                    internal_config = configuration.get('internal_repo')
                }

                extraVars['CONFIGURATION_INTERNAL_REPO'] = internal_config

                parameters{
                    stringParam('CONFIGURATION_REPO', extraVars.get('CONFIGURATION_REPO', 'https://github.com/edx/configuration.git'),
                                    'Git repo containing edX configuration.')
                    stringParam('CONFIGURATION_BRANCH', extraVars.get('CONFIGURATION_BRANCH', 'master'),
                            'e.g. tagname or origin/branchname')

                    stringParam('CONFIGURATION_INTERNAL_REPO', extraVars.get('CONFIGURATION_INTERNAL_REPO',  "git@github.com:edx/${deployment}-internal.git"),
                            'Git repo containing internal overrides')
                    stringParam('CONFIGURATION_INTERNAL_BRANCH', extraVars.get('CONFIGURATION_INTERNAL_BRANCH', 'master'),
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
                            url('$CONFIGURATION_INTERNAL_REPO')
                            branch('$CONFIGURATION_INTERNAL_BRANCH')
                                if (gitCredentialId) {
                                    credentials(gitCredentialId)
                                }
                        }
                        extensions {
                            cleanAfterCheckout()
                            pruneBranches()
                            relativeTargetDirectory('configuration-internal')
                        }
                    }
                }

                throttleConcurrentBuilds {
                    maxPerNode(0)
                    maxTotal(0)
                }

                triggers {
                    cron("H/10 * * * *")
                }

                environmentVariables {
                    env('REGION', extraVars.get('REGION','us-east-1'))
                }

                steps {
                    virtualenv {
                        nature("shell")
                        systemSitePackages(false)

                        command(
                            dslFactory.readFileFromWorkspace("devops/resources/cluster-instance-monitoring.sh")
                        )

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
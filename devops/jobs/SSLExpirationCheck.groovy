/*
 Variables without defaults are marked (required) 
 
 Variables consumed for this job:
    * DEPLOYMENTS (required)
        - deployment
    * FROM_ADDRESS: email address to send notifications that certificates are expiring
    * TO_ADDRESS: email address that sends the mail
    * SECURE_GIT_CREDENTIALS: secure-bot-user (required)
    * SYSADMIN_REPO: repository containing SSL expiration check python script (required)
    * SYSADMIN_BRANCH: default is master
    * CONFIGURATION_REPO: name of config repo, default is https://github.com/edx/configuration.git
    * CONFIGURATION_BRANCH: default is master
    * REGION: default is us-east-1
    * NOTIFY_ON_FAILURE: alert@example.com
    * FOLDER_NAME: folder, default is Monitoring
    * SNITCH: deadmans snitch
    * DAYS: alert if SSL certificate will expire within these days
    

 This job expects the following credentials to be defined on the folder
    tools-edx-jenkins-aws-credentials: file with key/secret in boto config format
    ssl-expiration-check-${deployment}-role-arn: the role to aws sts assume-role

*/

package devops.jobs
import static org.edx.jenkins.dsl.Constants.common_wrappers
import static org.edx.jenkins.dsl.Constants.common_logrotator

class SSLExpirationCheck{
    public static def job = { dslFactory, extraVars ->
        assert extraVars.containsKey('DEPLOYMENTS') : "Please define DEPLOYMENTS. It should be a list of strings."
        assert !(extraVars.get('DEPLOYMENTS') instanceof String) : "Make sure DEPLOYMENTS is a list and not a string"
        extraVars.get('DEPLOYMENTS').each { deployment ->
            
            dslFactory.job(extraVars.get("FOLDER_NAME","Monitoring") + "/ssl-expiration-check-${deployment}") {
                wrappers common_wrappers
                logRotator common_logrotator

                wrappers {
                    credentialsBinding {
                        file('AWS_CONFIG_FILE','tools-edx-jenkins-aws-credentials')
                        string('ROLE_ARN', "ssl-expiration-check-${deployment}-role-arn")
                    }
                }

                def gitCredentialId = extraVars.get('SECURE_GIT_CREDENTIALS','')
                assert extraVars.containsKey('SYSADMIN_REPO') : "Please define a system admin repo where the SSL expiration check  script is located"

                parameters{
                    stringParam('CONFIGURATION_REPO', extraVars.get('CONFIGURATION_REPO', 'https://github.com/edx/configuration.git'),
                            'Git repo containing edX configuration.')
                    stringParam('CONFIGURATION_BRANCH', extraVars.get('CONFIGURATION_BRANCH', 'master'),
                            'e.g. tagname or origin/branchname')
                    stringParam('SYSADMIN_REPO', extraVars.get('SYSADMIN_REPO'),
                            'Git repo containing sysadmin configuration which contains the ssl expiration check script.')
                    stringParam('SYSADMIN_BRANCH', extraVars.get('SYSADMIN_BRANCH', 'master'),
                            'e.g. tagname or origin/branchname')
                    stringParam('TO_ADDRESS', extraVars.get('TO_ADDRESS', ""))
                    stringParam('FROM_ADDRESS', extraVars.get('FROM_ADDRESS', ""))
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

                triggers {
                    cron("H 15 * * * ")
                }

                environmentVariables {
                    env('REGION', extraVars.get('REGION','us-east-1'))
                    env('DAYS', extraVars.get('DAYS', 30))
                    env('SNITCH', extraVars.get('SNITCH'))
                }

                steps {
                    virtualenv {
                        nature("shell")
                        systemSitePackages(false)

                        command(
                            dslFactory.readFileFromWorkspace("devops/resources/ssl-expiration-check.sh")
                        )

                    }

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

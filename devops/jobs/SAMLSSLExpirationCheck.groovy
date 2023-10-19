/*
 Variables without defaults are marked (required) 
 
 Variables consumed for this job:
    * DEPLOYMENTS (required)
         deployment:
            environments:
              environment (required)
                 saml_secret (required)
                 secret_key (required)
    * SECURE_GIT_CREDENTIALS: secure-bot-user (required)
    * SYSADMIN_REPO: repository containing SSL expiration check python script (required)
    * SYSADMIN_BRANCH: default is master
    * CONFIGURATION_REPO: name of config repo, default is https://github.com/edx/configuration.git
    * CONFIGURATION_BRANCH: default is master
    * REGION: default is us-east-1
    * NOTIFY_ON_FAILURE: alert@example.com
    * FOLDER_NAME: folder, default is Monitoring
    * DAYS: alert if SSL certificate will expire within these days

*/

package devops.jobs
import static org.edx.jenkins.dsl.Constants.common_logrotator

class SAMLSSLExpirationCheck{
    public static def job = { dslFactory, extraVars ->
        assert extraVars.containsKey('DEPLOYMENTS') : "Please define DEPLOYMENTS. It should be a list of strings."
        assert !(extraVars.get('DEPLOYMENTS') instanceof String) : "Make sure DEPLOYMENTS is a list and not a string"
        extraVars.get('DEPLOYMENTS').each { deployment, configuration ->
                configuration.environments.each { environment, inner_config -> 
                    dslFactory.job(extraVars.get("FOLDER_NAME","Monitoring") + "/saml-ssl-expiration-check-${environment}-${deployment}") {
                        logRotator common_logrotator


                        def gitCredentialId = extraVars.get('SECURE_GIT_CREDENTIALS','')
                        
                        assert extraVars.containsKey('SYSADMIN_REPO') : "Please define a system admin repo where the SSL expiration check  script is located"

                        parameters{
                        stringParam('SYSADMIN_REPO', extraVars.get('SYSADMIN_REPO'),
                            'Git repo containing sysadmin configuration which contains the ssl expiration check script.')
                        stringParam('SYSADMIN_BRANCH', extraVars.get('SYSADMIN_BRANCH', 'master'),
                            'e.g. tagname or origin/branchname')
                        }
                
                        multiscm{
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
                            env('DAYS', extraVars.get('DAYS', 90))
                            env('SAML_SECRET',inner_config.get('saml_secret'))
                            env('SECRET_KEY',inner_config.get('secret_key'))
                        }

                        steps {
                          shell(dslFactory.readFileFromWorkspace('devops/resources/saml-ssl-expiration-check.sh'))
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

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
    * MONITORING_SCRIPT_REPO: name of config repo, default is https://github.com/edx/configuration.git
    * MONITORING_SCRIPT_REPO_BRANCH: default is master
    * REGION: default is us-east-1
    * NOTIFY_ON_FAILURE: alert@example.com
    * FOLDER_NAME: folder, default is Monitoring
    * DAYS: alert if SSL certificate will expire within these days

*/

package devops.jobs
import static org.edx.jenkins.dsl.Constants.common_logrotator

class SAMLSSLExpirationCheck {
  public static def job = {
    dslFactory,
    extraVars ->
    assert extraVars.containsKey('DEPLOYMENTS'): "Please define DEPLOYMENTS. It should be a list of strings."
    assert!(extraVars.get('DEPLOYMENTS') instanceof String): "Make sure DEPLOYMENTS is a list and not a string"
    extraVars.get('DEPLOYMENTS').each {
      deployment,
      configuration ->
      configuration.environments.each {
        environment,
        inner_config ->
        dslFactory.job(extraVars.get("FOLDER_NAME", "Monitoring") + "/saml-ssl-expiration-check-${environment}-${deployment}") {
          logRotator common_logrotator

          def gitCredentialId = extraVars.get('SECURE_GIT_CREDENTIALS', '')

          parameters {
            stringParam('MONITORING_SCRIPTS_REPO', extraVars.get('MONITORING_SCRIPTS_REPO', 'git@github.com:edx/monitoring-scripts.git'),
              'Git repo containing edX monitoring scripts, which contains the ssl expiration check script.')
            stringParam('MONITORING_SCRIPTS_BRANCH', extraVars.get('MONITORING_SCRIPTS_BRANCH', 'master'),
              'e.g. tagname or origin/branchname')
          }

          multiscm {
            git {
              remote {
                url('$MONITORING_SCRIPTS_REPO')
                branch('$MONITORING_SCRIPTS_BRANCH')
                if (gitCredentialId) {
                  credentials(gitCredentialId)
                }
              }
              extensions {
                cleanAfterCheckout()
                pruneBranches()
                relativeTargetDirectory('monitoring-scripts')
              }
            }
          }

          triggers {
            cron("H 15 * * * ")
          }

          environmentVariables {
            env('REGION', extraVars.get('REGION', 'us-east-1'))
            env('DAYS', extraVars.get('DAYS', 90))
            env('SAML_SECRET', inner_config.get('saml_secret'))
            env('SECRET_KEY', inner_config.get('secret_key'))
          }

          steps {
            shell(dslFactory.readFileFromWorkspace('devops/resources/saml-ssl-expiration-check.sh'))
          }

          if (extraVars.get('NOTIFY_ON_FAILURE')) {
            publishers {
              mailer(extraVars.get('NOTIFY_ON_FAILURE'), false, false)
            }
          }

        }
      }
    }
  }
}

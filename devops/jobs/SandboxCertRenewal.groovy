package devops.jobs

import static org.edx.jenkins.dsl.Constants.common_wrappers
import static org.edx.jenkins.dsl.Constants.common_logrotator

class SandboxCertRenewal {
    public static def job = { dslFactory, extraVars ->
        assert extraVars.containsKey("DOMAIN") : "Please define DOMAIN (e.g., sandbox.edx.org)"

        def domain = extraVars.get("DOMAIN")

        dslFactory.job(extraVars.get("FOLDER_NAME","Monitoring") + "/sandbox-cert-renew") {
            parameters {
                stringParam('EMAIL', 'devops@edx.org', 'Email for certbot registration')
                stringParam('AWS_REGION', 'us-east-1', 'AWS region')
            }

            wrappers common_wrappers
            logRotator common_logrotator

            wrappers {
                credentialsBinding {
                    string("ROLE_ARN", "certbot-role-arn")
                }
            }

            environmentVariables {
                env('DOMAIN', domain)
            }

            multiscm{
                git {
                    remote {
                        url('https://github.com/edx/configuration.git')
                        branch('master')
                    }
                    extensions {
                        cleanAfterCheckout()
                        pruneBranches()
                        relativeTargetDirectory('configuration')
                    }
                }
            }

            triggers {
                cron("H H 1 */3 *") // Every 3 months
            }

            steps {
                       shell(dslFactory.readFileFromWorkspace('devops/resources/sandbox-cert-renew.sh'))
                    }
        }
    }
}

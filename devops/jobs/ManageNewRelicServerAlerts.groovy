/*
    Required Variables consumed by this job:
    * SERVER_ALERT_MAPPING: A map of server policy names to apps which belong to the server policy
    * SECURE_GIT_CREDENTIAL_ID - Id of the git credentials to use for cloning repos.
    * SYSADMIN_REPO - URL to the sysadmin repo.

    Optional Variables consumed by this job:
    * FOLDER_NAME - Name of folder to put the job in. (default: "Monitoring")
    * SYSADMIN_BRANCH - Branch of sysadmin repo to use. (default: 'master')
    * NOTIFY_ON_FAILURE - The e-mail addresses to notify on failure.

    Credentials required by this job:
    * new-relic-api-key: API key to be able to talk to NewRelic's API 
    * <secure_git_credential>: A SSH credential used to clone the git repos.
*/

package devops.jobs
import static org.edx.jenkins.dsl.Constants.common_wrappers
import static org.edx.jenkins.dsl.Constants.common_logrotator

class  ManageNewRelicServerAlerts {
    public static def job = { dslFactory, extraVars ->
        assert extraVars.containsKey("SERVER_ALERT_MAPPING")
        assert extraVars.containsKey("SECURE_GIT_CREDENTIAL_ID")
        assert extraVars.containsKey("SYSADMIN_REPO")

        extraVars.get("SERVER_ALERT_MAPPING").each { alertPolicyName, appNames ->
            dslFactory.job(extraVars.get("FOLDER_NAME", "Monitoring/") + "manage-newrelic-server-alerts-${alertPolicyName}") {
                logRotator common_logrotator
                wrappers common_wrappers

                wrappers {
                    credentialsBinding {
                        string('NEW_RELIC_API_KEY', 'new-relic-api-key')
                    }
                }

                def gitCredentialId = extraVars.get('SECURE_GIT_CREDENTIAL_ID')

                parameters{
                    stringParam('SYSADMIN_REPO', extraVars.get('SYSADMIN_REPO'),
                            'Git repo containing sysadmin configuration which contains the sandbox termination script.')
                    stringParam('SYSADMIN_BRANCH', extraVars.get('SYSADMIN_BRANCH', 'master'),
                            'e.g. tagname or origin/branchname')
                }

                multiscm {
                    git {
                        remote {
                            url('$SYSADMIN_REPO')
                            branch('$SYSADMIN_BRANCH')
                            credentials(gitCredentialId)
                        }

                        extensions {
                            cleanAfterCheckout()
                            pruneBranches()
                            relativeTargetDirectory('sysadmin')
                        }
                    }
                }

                triggers {
                    cron("H/5 * * * *")
                }

                environmentVariables {
                    env("ALERT_POLICY_NAME", alertPolicyName)
                    env("APP_NAMES", appNames.join(" "))
                }

                steps {
                    virtualenv {
                        pythonName('System-CPython-3.5')
                        nature("shell")
                        systemSitePackages(false)

                        command(
                            dslFactory.readFileFromWorkspace("devops/resources/manage-newrelic-server-alerts.sh")
                        )
                    }
                }

                if (extraVars.get("NOTIFY_ON_FAILURE")) {
                    publishers {
                    mailer(extraVars.get('NONTIFY_ON_FAILURE'), false, false)
                    }
                }
            }
        }
    }
}

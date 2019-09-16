/*
 Variables without defaults are marked (required) 
 
 Variables consumed for this job:
    * DEPLOYMENTS: (required)
          deployment:
            environments:
              -  environment
    * NOTIFY_ON_FAILURE: alert@example.com
    * FOLER_NAME: folder
    * RECIPIENTS: name@example.com (required)

 This job expects the following credentials to be defined on the folder
    ${environment}-${deployment}-mysql-credentials: secret file that contains the mysql credentials including user, password, and host

*/

package devops.jobs

import static org.edx.jenkins.dsl.Constants.common_wrappers

class StaffSuperAccountAudit {
    public static def job = { dslFactory, extraVars ->
        assert extraVars.containsKey('DEPLOYMENTS') : "Please define DEPLOYMENTS. It should be a list of strings."
        assert !(extraVars.get('DEPLOYMENTS') instanceof String) : "Make sure DEPLOYMENTS is a list and not a string"
        extraVars.get('DEPLOYMENTS').each { deployment, configuration ->
            configuration.environments.each { environment ->

                dslFactory.job(extraVars.get("FOLDER_NAME","User-Management") + "/staff-super-account-audit-${environment}-${deployment}") {

                    wrappers common_wrappers

                    wrappers {
                        credentialsBinding {
                            file('MYSQL_CONFIG_FILE',"${environment}-${deployment}-mysql-credentials")
                        }
                    }

                    triggers {
                        cron('@monthly')
                    }

                    environmentVariables {
                        env('ENVIRONMENT', environment)
                        env('DEPLOYMENT', deployment)
                    }

                    steps {
                        virtualenv {
                            pythonName('System-CPython-3.6')
                            nature("shell")
                            systemSitePackages(false)

                            command(
                                dslFactory.readFileFromWorkspace("devops/resources/staff-super-account-audit.sh")
                            )

                        }

                    }

                    assert extraVars.containsKey('RECIPIENTS') : "Please define RECIPIENTS."
                    assert extraVars.get('RECIPIENTS') != null : "Please make sure that a recipient for the results of this job is defined."
                    publishers{
                        extendedEmail{
                            recipientList(extraVars.get('RECIPIENTS'))
                            replyToList('')
                            defaultSubject("Staff/Superuser Audit for ${environment}-${deployment}")
                            defaultContent("Periodic staff and superuser audit for ${environment}-${deployment}. \nIf there is no attachment, this job has failed (staff-super-account-audit-${environment}-${deployment} on http://tools-edx-jenkins.edx.org)")
                            contentType('text/html')
                            triggers{
                                always{ 
                                    attachmentPatterns("**/${environment}_${deployment}_account_report.csv")
                                }
                            }
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
}

/*
    Variables consumed from the EXTRA_VARS input to your seed job in addition
    to those listed in the seed job.
    * JOB_FOLDER_NAME: some-folder
    * SSH_CREDENTIAL_NAME : your-ssh-credential-name
    * SSH_USERNAME : your-ssh-username
    * CONTACT_EMAIL : e-mail-contact@example.com
*/

package ora2.jobs

class BuildSandbox {

    public static def job = { dslFactory, extraVars ->
        return dslFactory.job(extraVars.get('JOB_FOLDER_NAME', 'ora2') + '/add-course-to-sandbox') {
            properties {
                parameters {
                    stringParam('SANDBOX_BASE', 'ora2', '')
                }
            }
            steps {
                downstreamParameterized {
                    triggers(extraVars.get('SANDBOX_JOB','Sandboxes/CreateSandbox')) {
                        parameters {
                            predefinedProps([
                                dns_name: '',

                            ])
                        }
                    }
                }
            }
            publishers {
                mailer(extraVars['CONTACT_EMAIL'], true, false)
            }
        }
    }
}

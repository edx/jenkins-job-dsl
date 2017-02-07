/*
    Variables consumed from the EXTRA_VARS input to your seed job in addition
    to those listed in the seed job.

    * JOB_FOLDER_NAME: some-folder
    * NOTIFY_ON_FAILURE : e-mail-contact@example.com
    * SANDBOX_JOB : 'Sandboxes/CreateSandbox'
*/

package ora2.jobs

class BuildOraSandbox {

    public static def job = { dslFactory, extraVars ->
        return dslFactory.job(extraVars.get('JOB_FOLDER_NAME', 'ora2') + '/build-ora-sandbox') {
            properties {
                parameters {
                    stringParam('dns_name', 'ora2', '')
                    stringParam('name_tag', 'ora2', '')
                    stringParam('configuration_version', 'master', '')
                    stringParam('configuration_source_repo', 'https://github.com/edx/configuration.git')
                    stringParam('configuration_secure_version', 'master', '')
                    stringParam('coniguration_internal_version', 'master', '')
                    stringParam('edxapp_version', 'master', '')
                    stringParam('edx_platform_repo', 'https://github.com/edx/edx-platform.git', '')
                }
            }
            steps {
                downstreamParameterized {
                    trigger(extraVars.get('SANDBOX_JOB','Sandboxes/CreateSandbox')) {
                        parameters {
                            currentBuild()
                            booleanParam('basic_auth', false)
                        }
                    }
                }
            }
            publishers {
                mailer(extraVars['NOTIFY_ON_FAILURE'], true, false)
            }
        }
    }
}

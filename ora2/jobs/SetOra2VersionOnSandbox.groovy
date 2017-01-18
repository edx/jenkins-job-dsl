/*
    Variables consumed from the EXTRA_VARS input to your seed job in addition
    to those listed in the seed job.

    FOLDER_NAME: some-folder
    NOTIFY_ON_FAILURE : e-mail-contact@example.com
*/

package ora2.jobs

class SetOra2VersionOnSandbox {

    public static def job = { dslFactory, extraVars ->
        def jobRoot = extraVars.get("FOLDER_NAME", 'ora2')
        def jobName = jobRoot + '/SetOra2VersionOnSandbox'

        return dslFactory.job(jobName) {
            parameters {

                stringParam('ORA2_VERSION',
                            'master',
                            'The version/branch name of ORA2 to fetch/checkout on target machine'
                            )
                stringParam("SANDBOX_HOST",
                            "",
                            "The host name and address of the Sandbox to be updated (ex. ora2.sandbox.edx.org)"
                           )
                stringParam("NOTIFY_ON_FAILURE",
                            extraVars.get("NOTIFY_ON_FAILURE", ),
                            "Email to notify on failure")
            }

            concurrentBuild(false)
            def ansibleJobName = jobRoot + '/oraVersion'
            def ora2_version_str = '$ORA2_VERSION'

            publishers{
                downstreamParameterized {
                    trigger(ansibleJobName) {
                        condition('SUCCESS')
                        parameters {
                            currentBuild()
                            predefinedProp('ANSIBLE_INVENTORY', '$SANDBOX_HOST')
                            predefinedProp('ANSIBLE_EXTRA_VARS', "-e ora2_version=${ora2_version_str}")
                            predefinedProp('ANSIBLE_PLAYBOOK', 'ora2.yml')
                        }
                    }
                }
            }
        }
    }
}

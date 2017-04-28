/*
    Variables consumed from the EXTRA_VARS input to your seed job in addition
    to those listed in the seed job.

    * BASIC_AUTH_PASS : some-password
    * BASIC_AUTH_USER : some-user
    * FOLDER_NAME: some-folder
    * NOTIFY_ON_FAILURE : e-mail-contact@example.com
*/

package ora2.jobs

class RunAcceptanceTests {

    public static def job = { dslFactory, extraVars ->
        return dslFactory.job(extraVars.get('FOLDER_NAME', 'ora2') + '/run-acceptance-tests') {
            description('Run ORA2 acceptance tests on a sandbox.')
            logRotator{
                daysToKeep(10)
                numToKeep(1)
                artifactDaysToKeep(1)
                artifactNumToKeep(1)
            }
            properties {
                githubProjectProperty {
                    projectUrlStr('https://github.com/edx/edx-ora2')
                }
                parameters {
                    stringParam(
                        'SANDBOX_HOST',
                        'ora2.sandbox.edx.org',
                        "The hostname of the sandbox."
                    )
                    stringParam(
                        'BRANCH',
                        'origin/master',
                        ''
                    )
                    stringParam(
                        'SLEEP_TIME',
                        '300',
                        'How long to wait before actually starting the tests (apparently this is necessary to ensure environment is ready?)'
                    )
                    stringParam(
                        "NOTIFY_ON_FAILURE",
                        extraVars.get("NOTIFY_ON_FAILURE", ''),
                        "Email to notify on failure"
                    )
                }
            }
            scm {
                git {
                    remote {
                        url('https://github.com/edx/edx-ora2.git')
                    }
                    branch('${BRANCH}')
                    extensions {
                        cleanCheckout()
                    }
                }
            }
            steps {
                environmentVariables {
                    env('BASIC_AUTH_USER', extraVars.get('BASIC_AUTH_USER'))
                    env('BASIC_AUTH_PASSWORD', extraVars.get('BASIC_AUTH_PASS'))
                    env('TEST_HOST', '${SANDBOX_HOST}')
                }
                virtualenv {
                    name('shell')
                    command(
                        dslFactory.readFileFromWorkspace('ora2/resources/run-ora2-acceptance-tests.sh')
                    )
                    clear()
                }
            }
            publishers {
                artifactArchiver {
                    artifacts('test/acceptance/screenshots/*.png,test/acceptance/xunit*.xml,test/logs/*')
                    allowEmptyArchive(true)
                    onlyIfSuccessful(false)
                    fingerprint(false)
                    defaultExcludes(true)
                    caseSensitive(true)
                }
                jUnitResultArchiver {
                    testResults("**/build/test-reports/*.xml")
                    allowEmptyResults(true)
                }
                mailer('$NOTIFY_ON_FAILURE', true, false)
            }
            wrappers {
                buildTimeoutWrapper {
                    strategy {
                        absoluteTimeOutStrategy {
                            timeoutMinutes('75')
                        }
                    }
                    timeoutEnvVar('')
                }
                timestamperBuildWrapper()
            }
        }
    }

}

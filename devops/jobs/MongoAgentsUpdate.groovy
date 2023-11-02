package devops.jobs
import static org.edx.jenkins.dsl.Constants.common_logrotator
import static org.edx.jenkins.dsl.Constants.common_wrappers

class MongoAgentsUpdate {
    public static def job = {
        dslFactory, extraVars ->
        dslFactory.job("Monitoring" + "/mongo-agents-update") {
            logRotator common_logrotator
            def gitCredentialId = extraVars.get('SECURE_GIT_CREDENTIALS','')
            parameters {
                stringParam('CONFIGURATION_REPO', 'https://github.com/edx/configuration.git')
                stringParam('CONFIGURATION_BRANCH', 'master')
                stringParam('CONFIGURATION_INTERNAL_REPO', extraVars.get('CONFIGURATION_INTERNAL_REPO',"git@github.com:edx/edx-internal.git"),
                    'Git repo containing internal overrides')
                stringParam('CONFIGURATION_INTERNAL_BRANCH', extraVars.get('CONFIGURATION_INTERNAL_BRANCH', 'master'),
                    'e.g. tagname or origin/branchname')
            }

            wrappers common_wrappers

            wrappers {
                        credentialsBinding {
                            def variable = "mongo-agents-update-arn"
                            string('ROLE_ARN', variable)
                        }
                        sshAgent(extraVars.get("SSH_AGENT_KEY"))
            }

            multiscm {
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
                        url('$CONFIGURATION_INTERNAL_REPO')
                        branch('$CONFIGURATION_INTERNAL_BRANCH')
                            if (gitCredentialId) {
                                credentials(gitCredentialId)
                            }
                    }
                    extensions {
                        cleanAfterCheckout()
                        pruneBranches()
                        relativeTargetDirectory('configuration-internal')
                    }
                }
            }

            steps {
                shell(dslFactory.readFileFromWorkspace('devops/resources/mongo-agents-update.sh'))
            }

            publishers {
                extendedEmail {
                    recipientList(extraVars.get('NOTIFY_ON_FAILURE'))
                    triggers {
                         failure {
                             attachBuildLog(false)  // build log contains PII!
                             compressBuildLog(false)  // build log contains PII!
                             subject('Failed build: ${JOB_NAME} #${BUILD_NUMBER}')
                             content('Jenkins job: ${JOB_NAME} failed.\n\nSee ${BUILD_URL} for details.')
                             contentType('text/plain')
                             sendTo {
                                 recipientList()
                             }
                         }
                    }
                }

            }
        }
    }
}

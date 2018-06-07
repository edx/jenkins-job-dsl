package devops.jobs
import static org.edx.jenkins.dsl.Constants.common_logrotator

class MongoAgentsUpdateCheck {
    public static def job = {
        dslFactory ->
        dslFactory.job("Monitoring" + "/mongo-agents-update-check") {
            parameters {
                stringParam('CONFIGURATION_REPO', 'https://github.com/edx/configuration.git')
                stringParam('CONFIGURATION_BRANCH', 'master')
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
            }

            triggers {
                cron('H 7 * * *')
            }

            steps {
                virtualenv {
                    pythonName('System-CPython-3.5')
                    nature("shell")
                    systemSitePackages(false)

                    command(
                        dslFactory.readFileFromWorkspace("devops/resources/mongo-agents-update-check.sh")
                    )
                }
            }

            publishers {

                extendedEmail {
                    recipientList('')
                    replyToList('')
                    contentType('text/plain')
                    defaultSubject('Mongo Agents Update is available')
                    defaultContent('''\
                        Please update the mongo monitoring/backup agents
                        See the installed version in 
                        https://github.com/edx/configuration/blob/master/playbooks/roles/mongo_mms/defaults/main.yml

                        $BUILD_URL
                        $BUILD_LOG
                        
                        '''.stripIndent())

                        triggers {
                            failure {}
                        }
                    }
                }
            }
     }
}

package analytics

import static org.edx.jenkins.dsl.AnalyticsConstants.common_log_rotator
import static org.edx.jenkins.dsl.AnalyticsConstants.common_publishers
import static org.edx.jenkins.dsl.AnalyticsConstants.common_triggers


class ExpireVerticaPassword {

    public static def job = { dslFactory, allVars ->

        dslFactory.job('expire-vertica-password') {

            // DENG-633
            disabled(true)

            logRotator common_log_rotator(allVars)
            parameters {
                stringParam('TOOLS_REPO', allVars.get('ANALYTICS_TOOLS_URL'), '')
                stringParam('TOOLS_BRANCH', allVars.get('ANALYTICS_TOOLS_BRANCH', 'origin/master'), 'e.g. tagname or origin/branchname')
                stringParam('CREDENTIALS', allVars.get('CREDENTIALS'))
                stringParam('EXCLUDE', allVars.get('EXCLUDE'))
                stringParam('MAPPING', allVars.get('MAPPING'))
                stringParam('NOTIFY', '$PAGER_NOTIFY', 'Space separated list of emails to send notifications to.')
            }
            multiscm {
                git {
                    remote {
                        url('$TOOLS_REPO')
                        branch('$TOOLS_BRANCH')
                        credentials('1')
                    }
                    extensions {
                        relativeTargetDirectory('analytics-tools')
                        pruneBranches()
                        cleanAfterCheckout()
                    }
                }
            }
            triggers common_triggers(allVars)
            wrappers {
                timestamps()
            }
            publishers common_publishers(allVars)
            steps {
                virtualenv {
                    pythonName('PYTHON_3.7')
                    nature("shell")
                    systemSitePackages(false)
                    command(
                        dslFactory.readFileFromWorkspace("dataeng/resources/expire-vertica-password.sh")
                    )
                }
            }
        }
    }
}

package analytics
import static org.edx.jenkins.dsl.AnalyticsConstants.secure_scm
import static org.edx.jenkins.dsl.AnalyticsConstants.common_log_rotator
import static org.edx.jenkins.dsl.AnalyticsConstants.common_wrappers
import static org.edx.jenkins.dsl.AnalyticsConstants.common_publishers
import static org.edx.jenkins.dsl.AnalyticsConstants.common_triggers
import static org.edx.jenkins.dsl.AnalyticsConstants.secure_scm_parameters

class DBTDocs{
    public static def job = { dslFactory, allVars ->
        dslFactory.job("dbt-docs"){
            logRotator common_log_rotator(allVars)
            parameters secure_scm_parameters(allVars)
            parameters {
                stringParam('WAREHOUSE_TRANSFORMS_URL', allVars.get('WAREHOUSE_TRANSFORMS_URL'), 'URL for the warehouse-transforms repository.')
                stringParam('WAREHOUSE_TRANSFORMS_BRANCH', allVars.get('WAREHOUSE_TRANSFORMS_BRANCH'), 'Branch of warehouse-transforms repository to use.')
                stringParam('DBT_TARGET', allVars.get('DBT_TARGET'), 'DBT target from profiles.yml in analytics-secure.')
                stringParam('DBT_PROFILE', allVars.get('DBT_PROFILE'), 'DBT profile from profiles.yml in analytics-secure.')
                stringParam('NOTIFY', allVars.get('NOTIFY','$PAGER_NOTIFY'), 'Space separated list of emails to send notifications to.')
            }
            multiscm secure_scm(allVars) << {
                git {
                    remote {
                        url('$WAREHOUSE_TRANSFORMS_URL')
                        branch('$WAREHOUSE_TRANSFORMS_BRANCH')
                        credentials('1')
                    }
                    extensions {
                        relativeTargetDirectory('warehouse-transforms')
                        pruneBranches()
                        cleanAfterCheckout()
                    }
                }
            }
            triggers common_triggers(allVars)
            wrappers {
                colorizeOutput('xterm')
            }
            wrappers common_wrappers(allVars)
            publishers common_publishers(allVars)
            steps {
                virtualenv {
                    pythonName('PYTHON_3.7')
                    nature("shell")
                    systemSitePackages(false)
                    command(
                        dslFactory.readFileFromWorkspace("dataeng/resources/dbt-docs.sh")
                    )
                }
            }
        }
    }
}


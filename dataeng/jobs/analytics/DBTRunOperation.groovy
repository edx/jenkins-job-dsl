package analytics

import static org.edx.jenkins.dsl.AnalyticsConstants.secure_scm
import static org.edx.jenkins.dsl.AnalyticsConstants.common_log_rotator
import static org.edx.jenkins.dsl.AnalyticsConstants.common_wrappers
import static org.edx.jenkins.dsl.AnalyticsConstants.common_publishers
import static org.edx.jenkins.dsl.AnalyticsConstants.common_triggers
import static org.edx.jenkins.dsl.AnalyticsConstants.secure_scm_parameters
import static org.edx.jenkins.dsl.AnalyticsConstants.common_authorization

class DBTRunOperation {
    public static def job = { dslFactory, allVars ->
        allVars.get('JOBS').each { job_name, job_config ->
            dslFactory.job("dbt-run-operation-$job_name"){
                description(
                    "This job is used to run the " + job_config('OPERATION_NAME') + 
                    " in the " + job_config('DBT_PROJECT') + " project"
                )
                authorization common_authorization(job_config)
                logRotator common_log_rotator(allVars)
                parameters secure_scm_parameters(allVars)
                parameters {
                    stringParam('WAREHOUSE_TRANSFORMS_URL', allVars.get('WAREHOUSE_TRANSFORMS_URL'), 'URL for the Warehouse Transforms Repo.')
                    stringParam('WAREHOUSE_TRANSFORMS_BRANCH', allVars.get('WAREHOUSE_TRANSFORMS_BRANCH'), 'Branch of Warehouse Transforms to use.')
                    stringParam('DBT_PROJECT', job_config.get('DBT_PROJECT', allVars.get('DBT_PROJECT')), 'dbt project in warehouse-transforms to work on.')
                    stringParam('DBT_PROFILE', job_config.get('DBT_PROFILE', allVars.get('DBT_PROFILE')), 'dbt profile from analytics-secure to work on.')
                    stringParam('DBT_TARGET', job_config.get('DBT_TARGET', allVars.get('DBT_TARGET')), 'dbt target from analytics-secure to work on.')
                    stringParam('OPERATION_NAME', job_config.get('OPERATION_NAME'), 'Name of the operation/macro to run')
                    stringParam('RUN_ARGS', job_config.get('RUN_ARGS'), 'Additional args to supply to the dbt command')
                    stringParam('NOTIFY', job_config.get('NOTIFY', allVars.get('NOTIFY','$PAGER_NOTIFY')), 'Space separated list of emails to send notifications to.')
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
                triggers common_triggers(allVars, job_config)
                wrappers common_wrappers(allVars)
                wrappers {
                    colorizeOutput('xterm')
                }
                publishers common_publishers(allVars)
                steps {
                    virtualenv {
                        pythonName('PYTHON_3.7')
                        nature("shell")
                        systemSitePackages(false)
                        command(
                            dslFactory.readFileFromWorkspace("dataeng/resources/dbt-run-operation.sh")
                        )
                    }
                }
            }
        }
    }
}

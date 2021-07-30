package analytics
import static org.edx.jenkins.dsl.AnalyticsConstants.secure_scm
import static org.edx.jenkins.dsl.AnalyticsConstants.common_authorization
import static org.edx.jenkins.dsl.AnalyticsConstants.common_log_rotator
import static org.edx.jenkins.dsl.AnalyticsConstants.common_wrappers
import static org.edx.jenkins.dsl.AnalyticsConstants.common_publishers
import static org.edx.jenkins.dsl.AnalyticsConstants.common_triggers
import static org.edx.jenkins.dsl.AnalyticsConstants.secure_scm_parameters

class DBTRun{
    public static def job = { dslFactory, allVars ->
        dslFactory.job("dbt-manual"){
            description(
                "Manually run dbt <strong>in production</strong>, overwriting data in the PROD database." +
                "<br><br>" +
                "DS&A and others may need to use this to populate or fix broken models before the usual automation " +
                "picks them up, or to run models that aren't a part of any automation such as the finrep archives, etc."
            )
            authorization common_authorization(allVars)
            logRotator common_log_rotator(allVars)
            parameters secure_scm_parameters(allVars)
            parameters {
                stringParam('WAREHOUSE_TRANSFORMS_URL', allVars.get('WAREHOUSE_TRANSFORMS_URL'), 'URL for the warehouse-transforms repository.')
                stringParam('WAREHOUSE_TRANSFORMS_BRANCH', allVars.get('WAREHOUSE_TRANSFORMS_BRANCH'), 'Branch of warehouse-transforms repository to use.')
                stringParam('DBT_TARGET', allVars.get('DBT_TARGET'), 'DBT target from profiles.yml in analytics-secure.')
                stringParam('DBT_PROFILE', allVars.get('DBT_PROFILE'), 'DBT profile from profiles.yml in analytics-secure.')
                stringParam('DBT_PROJECT_PATH', allVars.get('DBT_PROJECT_PATH'), 'Path in warehouse-transforms to use as the dbt project, relative to "projects" (usually automated/applications or reporting).')
                stringParam('DBT_RUN_OPTIONS', allVars.get('DBT_RUN_OPTIONS'), 'Additional options to dbt run/test, such as --models for model selection. Details here: https://docs.getdbt.com/docs/model-selection-syntax')
                stringParam('DBT_RUN_EXCLUDE', allVars.get('DBT_RUN_EXCLUDE'), 'Models to exlude from this run, the default is known incremental models. Leave these if you are not explicitly updating them!')
                stringParam('NOTIFY', allVars.get('NOTIFY','$PAGER_NOTIFY'), 'Space separated list of emails to send notifications to.')
                stringParam('JOB_TYPE', allVars.get('JOB_TYPE'), 'Pass parameter value of manual')
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
                        dslFactory.readFileFromWorkspace("dataeng/resources/dbt-run.sh")
                    )
                }
            }
        }
        dslFactory.job("dbt-automated"){
            description(
                "Automatically run dbt <strong>in production</strong>, overwriting data in the PROD database when Schema Builder generated PR are merged"
            )
            logRotator common_log_rotator(allVars)
            parameters secure_scm_parameters(allVars)
            environmentVariables {
                env('WAREHOUSE_TRANSFORMS_URL', allVars.get('WAREHOUSE_TRANSFORMS_URL'))
                env('WAREHOUSE_TRANSFORMS_BRANCH', allVars.get('WAREHOUSE_TRANSFORMS_BRANCH'))
                env('DBT_TARGET', allVars.get('DBT_TARGET'))
                env('DBT_PROFILE', allVars.get('DBT_PROFILE'))
                env('DBT_PROJECT_PATH', 'automated/applications')
                env('DBT_RUN_OPTIONS', allVars.get('DBT_RUN_OPTIONS'))
                env('DBT_RUN_EXCLUDE', allVars.get('DBT_RUN_EXCLUDE'))
                env('JOB_TYPE', 'automated')
                env('NOTIFY', allVars.get('$PAGER_NOTIFY'))
            }
            multiscm secure_scm(allVars) << {
                git {
                    remote {
                        url('$WAREHOUSE_TRANSFORMS_URL')
                        branch('$WAREHOUSE_TRANSFORMS_BRANCH')
                    }
                    extensions {
                        relativeTargetDirectory('warehouse-transforms')
                        pruneBranches()
                        cleanAfterCheckout()
                    }
                }
            }
            triggers {
                scm('H/2 * * * *')
            }
            wrappers {
                colorizeOutput('xterm')
                timestamps()
                credentialsBinding {
                    usernamePassword('GITHUB_USER', 'GITHUB_TOKEN', 'GITHUB_USER_PASS_COMBO');
                }
            }
            wrappers common_wrappers(allVars)
            publishers common_publishers(allVars)
            steps {
                virtualenv {
                    pythonName('PYTHON_3.7')
                    nature("shell")
                    systemSitePackages(false)
                    command(
                        dslFactory.readFileFromWorkspace("dataeng/resources/dbt-run.sh")
                    )
                }
            }
        }
    }
}

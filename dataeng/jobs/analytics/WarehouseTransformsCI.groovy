package analytics
import static org.edx.jenkins.dsl.AnalyticsConstants.secure_scm
import static org.edx.jenkins.dsl.AnalyticsConstants.common_log_rotator
import static org.edx.jenkins.dsl.AnalyticsConstants.common_wrappers
import static org.edx.jenkins.dsl.AnalyticsConstants.common_publishers
import static org.edx.jenkins.dsl.AnalyticsConstants.common_triggers
import static org.edx.jenkins.dsl.AnalyticsConstants.secure_scm_parameters
import static org.edx.jenkins.dsl.AnalyticsConstants.common_authorization
import static org.edx.jenkins.dsl.JenkinsPublicConstants.GHPRB_CANCEL_BUILDS_ON_UPDATE

class WarehouseTransformsCI{
    public static def job = { dslFactory, allVars ->
        dslFactory.job("warehouse-transforms-ci"){
            authorization common_authorization(allVars)
            logRotator common_log_rotator(allVars)
            parameters secure_scm_parameters(allVars)
            parameters {
                stringParam('WAREHOUSE_TRANSFORMS_URL', allVars.get('WAREHOUSE_TRANSFORMS_URL'), 'URL for the warehouse-transforms repository.')
                stringParam('WAREHOUSE_TRANSFORMS_BRANCH', allVars.get('WAREHOUSE_TRANSFORMS_BRANCH'), 'Branch of warehouse-transforms repository to use.')
                stringParam('PROJECT_URL', allVars.get('PROJECT_URL'), 'Github Project URL necessary to give when using GHPRB plugin.')
                stringParam('DBT_TARGET', allVars.get('DBT_TARGET'), 'DBT target from profiles.yml in analytics-secure.')
                stringParam('DBT_PROFILE', allVars.get('DBT_PROFILE'), 'DBT profile from profiles.yml in analytics-secure.')
                stringParam('DBT_PROJECT_PATH', allVars.get('DBT_PROJECT_PATH'), 'Path in warehouse-transforms to use as the dbt project, relative to "projects" (usually automated/applications or reporting).')
                stringParam('DBT_RUN_OPTIONS', allVars.get('DBT_RUN_OPTIONS'), 'Additional options to dbt run, such as --models for model selection. Details here: https://docs.getdbt.com/docs/model-selection-syntax')
                stringParam('DBT_RUN_EXCLUDE', allVars.get('DBT_RUN_EXCLUDE'), 'Additional options to dbt run, such as --exclude. Details here: https://docs.getdbt.com/docs/model-selection-syntax')
                stringParam('DBT_TEST_OPTIONS', allVars.get('DBT_TEST_OPTIONS'), 'Additional options to dbt test, such as --models for model selection. Details here: https://docs.getdbt.com/docs/model-selection-syntax')
                stringParam('DBT_TEST_EXCLUDE', allVars.get('DBT_TEST_EXCLUDE'), 'Additional options to dbt test, such as --exclude. Details here: https://docs.getdbt.com/docs/model-selection-syntax')
                stringParam('ANALYTICS_TOOLS_URL', allVars.get('ANALYTICS_TOOLS_URL'), 'URL for the analytics tools repo.')
                stringParam('ANALYTICS_TOOLS_BRANCH', allVars.get('ANALYTICS_TOOLS_BRANCH'), 'Branch of analytics tools repo to use.')
                stringParam('JENKINS_JOB_DSL_URL', allVars.get('JENKINS_JOB_DSL_URL'), 'URL for the jenkins-job-dsl repo.')
                stringParam('JENKINS_JOB_DSL_BRANCH', allVars.get('JENKINS_JOB_DSL_BRANCH'), 'Branch of jenkins-job-dsl repo to use.')
                stringParam('DB_NAME', allVars.get('DB_NAME'), 'Database name used to create output schema of dbt run/tests')
                stringParam('NOTIFY', allVars.get('NOTIFY','$PAGER_NOTIFY'), 'Space separated list of emails to send notifications to.')
            }
            environmentVariables {
                env('KEY_PATH', allVars.get('KEY_PATH'))
                env('PASSPHRASE_PATH', allVars.get('PASSPHRASE_PATH'))
                env('USER', allVars.get('USER'))
                env('ACCOUNT', allVars.get('ACCOUNT'))
            }
            scm {
                 github('edx/warehouse-transforms')
            }                      
            multiscm secure_scm(allVars) << {
                git {
                    remote {
                        url('$WAREHOUSE_TRANSFORMS_URL')
                        refspec('+refs/pull/*:refs/remotes/origin/pr/*')
                        credentials('1') 
                    }
                    branches('\${ghprbActualCommit}')
                    extensions {
                        relativeTargetDirectory('warehouse-transforms')
                        pruneBranches()
                        cleanAfterCheckout()
                    }
                }
                git {
                    remote {
                        url('$ANALYTICS_TOOLS_URL')
                        branch('$ANALYTICS_TOOLS_BRANCH')
                        credentials('1')
                    }
                    extensions {
                        relativeTargetDirectory('analytics-tools')
                        pruneBranches()
                        cleanAfterCheckout()
                    }
                }  
                git {
                    remote {
                        url('$JENKINS_JOB_DSL_URL')
                        branch('$JENKINS_JOB_DSL_BRANCH')
                        credentials('1')
                    }
                    extensions {
                        relativeTargetDirectory('jenkins-job-dsl')
                        pruneBranches()
                        cleanAfterCheckout()
                    }
                }                              
            }
            triggers {
                githubPullRequest {
                    // since the server running this job will not be publicly available,
                    // we cannot rely on Github to deliver webhooks. Instead, poll GH
                    // every 3 minutes for updates any branches.
                    cron('H/3 * * * *')
                    triggerPhrase('jenkins run dbt') // You this trigger phrase to on Pull Rquest comment to trigger this job
                    onlyTriggerPhrase(false) // true if you want the job to only fire when commented on (not on commits)
                    orgWhitelist(['edx-ops', 'edX']) // All the Github users under these orgs will be able to trigger this job via PR. As this job will be used by many edXers so giving the trigger access to all under edX.  
                }
            }
            configure GHPRB_CANCEL_BUILDS_ON_UPDATE(false)    

            wrappers {
                colorizeOutput('xterm')
            }
            wrappers common_wrappers(allVars)
            steps {
                virtualenv {
                    pythonName('PYTHON_3.7')
                    nature("shell")
                    systemSitePackages(false)
                    command(
                        dslFactory.readFileFromWorkspace("dataeng/resources/warehouse-transforms-ci.sh")
                    )
                }
            }
        }
    }
}


package analytics
import static org.edx.jenkins.dsl.AnalyticsConstants.secure_scm
import static org.edx.jenkins.dsl.AnalyticsConstants.common_log_rotator
import static org.edx.jenkins.dsl.AnalyticsConstants.common_wrappers
import static org.edx.jenkins.dsl.AnalyticsConstants.common_publishers
import static org.edx.jenkins.dsl.AnalyticsConstants.common_triggers
import static org.edx.jenkins.dsl.AnalyticsConstants.secure_scm_parameters
import static org.edx.jenkins.dsl.JenkinsPublicConstants.GHPRB_CANCEL_BUILDS_ON_UPDATE
// need to change the JenkinsPublicConstants.GHPRB_CANCEL_BUILDS_ON_UPDATE import
// Copied from DBTDocs.groovy 
// This is work in progress script
class WarehouseTransformsCI{
    public static def job = { dslFactory, allVars ->
        dslFactory.job("warehouse-transforms-ci"){
            logRotator common_log_rotator(allVars)
            parameters secure_scm_parameters(allVars)
            parameters {
                stringParam('WAREHOUSE_TRANSFORMS_URL', allVars.get('WAREHOUSE_TRANSFORMS_URL'), 'URL for the warehouse-transforms repository.')
                stringParam('WAREHOUSE_TRANSFORMS_BRANCH', allVars.get('WAREHOUSE_TRANSFORMS_BRANCH'), 'Branch of warehouse-transforms repository to use.')
                stringParam('PROJECT_URL', allVars.get('PROJECT_URL'), 'Github Project URL necessary to give when using GHPRB plugin.')
                stringParam('DBT_TARGET', allVars.get('DBT_TARGET'), 'DBT target from profiles.yml in analytics-secure.')
                stringParam('DBT_PROFILE', allVars.get('DBT_PROFILE'), 'DBT profile from profiles.yml in analytics-secure.')
                stringParam('DBT_PROJECT_PATH', allVars.get('DBT_PROJECT_PATH'), 'Path in warehouse-transforms to use as the dbt project, relative to "projects" (usually automated/applications or reporting).')
                stringParam('DBT_RUN_OPTIONS', allVars.get('DBT_RUN_OPTIONS'), 'Additional options to dbt run/test, such as --models for model selection. Details here: https://docs.getdbt.com/docs/model-selection-syntax')
                stringParam('ANALYTICS_TOOLS_URL', allVars.get('ANALYTICS_TOOLS_URL'), 'URL for the analytics tools repo.')
                stringParam('ANALYTICS_TOOLS_BRANCH', allVars.get('ANALYTICS_TOOLS_BRANCH'), 'Branch of analytics tools repo to use.')
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
                        //github('$PROJECT_URL') Not working for provding github project url
                        //github('$PROJECT_URL','ssh', 'github.com')
                        //refspec('+refs/pull/*:refs/remotes/origin/pr/*')
                        //branch('$WAREHOUSE_TRANSFORMS_BRANCH') // how to get the branch for which PR is raised - ans: either use sha1 or ghprbActualCommit 
                        refspec('+refs/pull/*:refs/remotes/origin/pr/*')
                        branch('\${sha1}')
                        credentials('1') 
                    }
                    //branch('\${ghprbActualCommit}')
                    extensions {
                        //cleanBeforeCheckout()
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
            }
            //triggers common_triggers(allVars)  // not using them as we are using github pull reuqest builder
            triggers {
                githubPullRequest {
                    // since the server running this job will not be publicly available,
                    // we cannot rely on Github to deliver webhooks. Instead, poll GH
                    // every 5 minutes for updates any branches.
                    cron('H/2 * * * *')
                    // useGitHubHooks() // Not using webhooks
                    triggerPhrase('jenkins run dbt')
                    onlyTriggerPhrase(true) // true if you want the job to only fire when commented on (not on commits)
                    userWhitelist(['jazibhumayun', 'hassanjaveed84']) // which GH users can run this // Need to update this later
                }
            }
            configure GHPRB_CANCEL_BUILDS_ON_UPDATE(false)   // saw from general PR Pipeline     

            wrappers {
                colorizeOutput('xterm')
            }
            wrappers common_wrappers(allVars)
            //publishers common_publishers(allVars)
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


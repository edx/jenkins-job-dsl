package org.edx.jenkins.dsl

class AnalyticsConstants {

    public static def common_log_rotator = { extraVars ->
        return {
            daysToKeep(30)
        }
    }

    public static def common_multiscm = { extraVars ->
        return {
            git {
                remote {
                    url('$TASKS_REPO')
                    branch('$TASKS_BRANCH')
                }
                extensions {
                    relativeTargetDirectory('analytics-tasks')
                    pruneBranches()
                    cleanAfterCheckout()
                }
            }
            git {
                remote {
                    url('$CONFIG_REPO')
                    branch('$CONFIG_BRANCH')
                }
                extensions {
                    relativeTargetDirectory('analytics-configuration')
                    pruneBranches()
                    cleanAfterCheckout()
                }
            }
/*            git {
                remote {
                    url('$SECURE_REPO')
                    branch('$SECURE_BRANCH')
                }
                extensions {
                    relativeTargetDirectory('analytics-secure')
                    pruneBranches()
                    cleanAfterCheckout()
                }
            }
*/        }
    }

    public static def common_parameters = { extraVars ->
        return {
            stringParam('CLUSTER_NAME', extraVars.get('CLUSTER_NAME'), 'Name of the EMR cluster to use for this job.')
            stringParam('CONFIG_BRANCH', '$ANALYTICS_CONFIGURATION_RELEASE', 'e.g. tagname or origin/branchname, or $ANALYTICS_CONFIGURATION_RELEASE')
            stringParam('CONFIG_REPO', 'git@github.com:edx/edx-analytics-configuration.git', '')
            textParam('EMR_EXTRA_VARS', extraVars.get('EMR_EXTRA_VARS'),'')
            stringParam('NOTIFY', '$PAGER_NOTIFY', 'Number of EMR instances to use for this job.')
            stringParam('NUM_TASK_CAPACITY', extraVars.get('NUM_TASK_CAPACITY'), 'Number of EMR instance capacity to use for this job.')
            stringParam('SECURE_BRANCH', '$ANALYTICS_SECURE_RELEASE', 'e.g. tagname or origin/branchname, or $ANALYTICS_SECURE_RELEASE when released.')
            stringParam('SECURE_CONFIG', 'analytics-tasks/prod-edx.cfg', '')
            stringParam('SECURE_REPO', extraVars.get('SECURE_REPO_URL'), '')
            stringParam('TASKS_BRANCH', '$ANALYTICS_PIPELINE_RELEASE', 'e.g. tagname or origin/branchname,  e.g. origin/master or $ANALYTICS_PIPELINE_RELEASE')
            stringParam('TASKS_REPO', 'https://github.com/edx/edx-analytics-pipeline.git', 'Git repo containing the analytics pipeline tasks.')
            stringParam('TASK_USER', extraVars.get('TASK_USER'), 'User which runs the analytics task on the EMR cluster.')
            booleanParam('TERMINATE', true, 'Terminate the EMR cluster after running the analytics task?')
        }
    }

    public static def common_wrappers = { extraVars ->
        return {
            sshAgent('1')
            timestamps()
        }
    }

    public static def common_publishers = { extraVars ->
        return {
            mailer('$NOTIFY')
        }
    }
}

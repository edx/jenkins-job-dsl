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
            stringParam('EMR_MASTER_INSTANCE_TYPE', extraVars.get('EMR_MASTER_INSTANCE_TYPE', 'm4.2xlarge'), 'EC2 Instance type used for master.')
            stringParam('EMR_WORKER_INSTANCE_TYPE_1', extraVars.get('EMR_WORKER_INSTANCE_TYPE_1', 'm4.2xlarge'), 'EC2 instance type used by workers.')
            stringParam('EMR_WORKER_INSTANCE_TYPE_2', extraVars.get('EMR_WORKER_INSTANCE_TYPE_2', 'm4.4xlarge'), 'EC2 instance type used by workers.')
            textParam('EMR_HADOOP_ENV_CONFIG', extraVars.get('EMR_HADOOP_ENV_CONFIG'), 'EMR Hadoop env configuration.')
            textParam('EMR_MAPRED_SITE_CONFIG', extraVars.get('EMR_MAPRED_SITE_CONFIG'), 'EMR mapred-site configuration')
            textParam('EMR_YARN_SITE_CONFIG', extraVars.get('EMR_YARN_SITE_CONFIG'), 'EMR yarn-site configuration.')
            textParam('EXTRA_VARS', extraVars.get('EMR_EXTRA_VARS'), $/Extra variables to pass to the EMR provision/terminate ansible playbook.
This text may reference other parameters in the task as shell variables, e.g.  $$CLUSTER_NAME./$)
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

    public static def date_interval_parameters = { extraVars ->
      return {
        stringParam('FROM_DATE', extraVars.get('FROM_DATE', '2013-11-01'),
          'The first date to export data for. Data for this date and all days before the "TO_DATE" parameter' +
          ' will be included.' + $/
  Format: A string that can be parsed by the GNU coreutils "date" utility.

  Examples:
  * today
  * yesterday
  * 3 days ago
  * 1 week ago
  * 2013-11-01
  /$)
        stringParam('TO_DATE', extraVars.get('TO_DATE', 'today'),
          'The day after the last date to export data for. Data from the "FROM_DATE" parameter to 11:59:59' +
          ' on the date before this date will be included.' + $/
  Format: A string that can be parsed by the GNU coreutils "date" utility.

  Examples:
  * today
  * yesterday
  * 3 days ago
  * 1 week ago
  * 2017-11-01
  /$)
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

package org.edx.jenkins.dsl

class AnalyticsConstants {

    public static def common_log_rotator = { allVars, env=[:] ->
        def job_frequency = env.get('DAYS_TO_KEEP_BUILD', allVars.get('DAYS_TO_KEEP_BUILD'))
        if (job_frequency) {
            return {
                daysToKeep(job_frequency.toInteger())
            }
        }
    }

    public static def common_multiscm = { allVars ->
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
        }
    }

    public static def secure_scm = { allVars ->
        return {
            git {
                remote {
                    url('$SECURE_REPO')
                    branch('$SECURE_BRANCH')
                    credentials('1')
                }
                extensions {
                    pruneBranches()
                    relativeTargetDirectory('analytics-secure')
                }
            }
        }
    }

    public static def data_czar_keys_scm = { allVars ->
        return {
            git {
                remote {
                    url(allVars.get('DATA_CZAR_KEYS_REPO_URL'))
                    branch('$DATA_CZAR_KEYS_BRANCH')
                    credentials('1')
                }
                extensions {
                    relativeTargetDirectory('data-czar-keys')
                }
            }
        }
    }

    public static def common_parameters = { allVars, env=[:] ->
        def parameters = {
            stringParam('CLUSTER_NAME', env.get('CLUSTER_NAME', allVars.get('CLUSTER_NAME')), 'Name of the EMR cluster to use for this job.')
            stringParam('CONFIG_BRANCH', '$ANALYTICS_CONFIGURATION_RELEASE', 'e.g. tagname or origin/branchname, or $ANALYTICS_CONFIGURATION_RELEASE')
            stringParam('CONFIG_REPO', 'git@github.com:edx/edx-analytics-configuration.git', '')
            stringParam('EMR_MASTER_INSTANCE_TYPE', env.get('EMR_MASTER_INSTANCE_TYPE', allVars.get('EMR_MASTER_INSTANCE_TYPE','m4.2xlarge')), 'EC2 Instance type used for master.')
            stringParam('EMR_WORKER_INSTANCE_TYPE_1', env.get('EMR_WORKER_INSTANCE_TYPE_1', allVars.get('EMR_WORKER_INSTANCE_TYPE_1','m4.2xlarge')), 'EC2 instance type used by workers.')
            stringParam('EMR_WORKER_INSTANCE_TYPE_2', env.get('EMR_WORKER_INSTANCE_TYPE_2', allVars.get('EMR_WORKER_INSTANCE_TYPE_2','m4.4xlarge')), 'EC2 instance type used by workers.')
            textParam('EMR_HADOOP_ENV_CONFIG', allVars.get('EMR_HADOOP_ENV_CONFIG'), 'EMR Hadoop env configuration.')
            textParam('EMR_MAPRED_SITE_CONFIG', allVars.get('EMR_MAPRED_SITE_CONFIG'), 'EMR mapred-site configuration')
            textParam('EMR_YARN_SITE_CONFIG', allVars.get('EMR_YARN_SITE_CONFIG'), 'EMR yarn-site configuration.')
            stringParam('EMR_APPLICATIONS_CONFIG', allVars.get('EMR_APPLICATIONS_CONFIG'), 'Applications to install on EMR cluster.')
            textParam('EMR_ADDITIONAL_STEPS', allVars.get('EMR_ADDITIONAL_STEPS', ''), 'Additional EMR steps')
            textParam('EXTRA_VARS', allVars.get('EMR_EXTRA_VARS'), $/Extra variables to pass to the EMR provision/terminate ansible playbook.
This text may reference other parameters in the task as shell variables, e.g.  $$CLUSTER_NAME./$)
            stringParam('NOTIFY', allVars.get('NOTIFY','$PAGER_NOTIFY'), 'Space separated list of emails to send notifications to.')
            stringParam('NUM_TASK_CAPACITY', env.get('NUM_TASK_CAPACITY', allVars.get('NUM_TASK_CAPACITY')), 'Number of EMR instance capacity to use for this job.')
            stringParam('SECURE_CONFIG', env.get('SECURE_CONFIG', allVars.get('SECURE_CONFIG', 'analytics-tasks/prod-edx.cfg')), '')
            stringParam('TASKS_BRANCH', '$ANALYTICS_PIPELINE_RELEASE', 'e.g. tagname or origin/branchname,  e.g. origin/master or $ANALYTICS_PIPELINE_RELEASE')
            stringParam('TASKS_REPO', 'https://github.com/edx/edx-analytics-pipeline.git', 'Git repo containing the analytics pipeline tasks.')
            stringParam('TASK_USER', allVars.get('TASK_USER'), 'User which runs the analytics task on the EMR cluster.')
            booleanParam('TERMINATE', true, 'Terminate the EMR cluster after running the analytics task?')
        }
        // secure_scm_parameters provides variables required by run-automated-task.sh.
        return parameters >> AnalyticsConstants.secure_scm_parameters(allVars)
    }

    // Include this whenever secure_scm() is used, or when run-automated-task.sh is executed in a shell command.
    public static def secure_scm_parameters = { allVars ->
        return {
            stringParam('SECURE_BRANCH', '$ANALYTICS_SECURE_RELEASE', 'e.g. tagname or origin/branchname, or $ANALYTICS_SECURE_RELEASE when released.')
            stringParam('SECURE_REPO', allVars.get('SECURE_REPO_URL'), '')
        }
    }

    public static def from_date_interval_parameter = { allVars ->
      return {
        stringParam('FROM_DATE', allVars.get('FROM_DATE', '2013-11-01'),
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
      }
    }

    public static def to_date_interval_parameter = { allVars ->
      return {
        stringParam('TO_DATE', allVars.get('TO_DATE', 'today'),
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

    public static def common_wrappers = { allVars ->
        return {
            sshAgent('1')
            timestamps()
        }
    }

    public static def common_publishers = { allVars ->
        return {
            mailer('$NOTIFY')
        }
    }

    public static def common_triggers = { allVars, env=[:] ->
        def job_frequency = env.get('JOB_FREQUENCY', allVars.get('JOB_FREQUENCY'))
        if (job_frequency) {
            return {
                cron(job_frequency)
            }
        }
    }
}

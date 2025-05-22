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
        def tasks_scm = {
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
        }
        return tasks_scm >> AnalyticsConstants.analytics_configuration_scm(allVars)
    }

    public static def analytics_configuration_scm = { allVars ->
        return {
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
            stringParam('CONFIG_BRANCH', '$ANALYTICS_CONFIGURATION_RELEASE', 'e.g. tagname or origin/branchname, or $ANALYTICS_CONFIGURATION_RELEASE')
            stringParam('CONFIG_REPO', 'git@github.com:edx/edx-analytics-configuration.git', '')
            stringParam('NOTIFY', allVars.get('NOTIFY','$PAGER_NOTIFY'), 'Space separated list of emails to send notifications to.')
            stringParam('SECURE_CONFIG', env.get('SECURE_CONFIG', allVars.get('SECURE_CONFIG', 'analytics-tasks/prod-edx.cfg')), '')
            stringParam('TASKS_BRANCH', allVars.get('TASKS_BRANCH', '$ANALYTICS_PIPELINE_RELEASE'), 'e.g. tagname or origin/branchname,  e.g. origin/master or $ANALYTICS_PIPELINE_RELEASE')
            stringParam('TASKS_REPO', 'https://github.com/edx/edx-analytics-pipeline.git', 'Git repo containing the analytics pipeline tasks.')
            stringParam('TASK_USER', allVars.get('TASK_USER'), 'User which runs the analytics task on the EMR cluster.')
            booleanParam('TERMINATE', true, 'Terminate the EMR cluster after running the analytics task?')
            stringParam('EXTRA_ARGS', env.get('EXTRA_ARGS', allVars.get('EXTRA_ARGS', '')), 'Extra arguments that will be passed to tasks.')
            stringParam('PYTHON_VERSION',
                        env.get('PYTHON_VERSION',
                        allVars.get('PYTHON_VERSION')),
                        'Path to a specific python interpreter inside the EMR cluster that will be used to run pipelines tasks.')
            stringParam('BUILD_STATUS')
        }
        // secure_scm_parameters provides variables required by run-automated-task.sh.
        return parameters >> AnalyticsConstants.secure_scm_parameters(allVars) >> AnalyticsConstants.emr_cluster_parameters(allVars, env)
    }

    public static def emr_cluster_parameters = { allVars, env=[:] ->
        return {
            stringParam('CLUSTER_NAME', env.get('CLUSTER_NAME', allVars.get('CLUSTER_NAME')), 'Name of the EMR cluster to use for this job.')
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
            stringParam('NUM_TASK_CAPACITY', env.get('NUM_TASK_CAPACITY', allVars.get('NUM_TASK_CAPACITY')), 'Number of EMR spot instance capacity to use for this job.')
            stringParam('ENV_NAME', env.get('ENV_NAME', allVars.get('ENV_NAME')), 'Env name e.g. prod, edge or stage for EMR tags.')
            stringParam('ON_DEMAND_CAPACITY', env.get('ON_DEMAND_CAPACITY', allVars.get('ON_DEMAND_CAPACITY')), 'Number of EMR on-demand instance capacity to use for this job.')
            stringParam('USE_SPOT', env.get('USE_SPOT', allVars.get('USE_SPOT')), 'Whether to use spot instances for the EMR cluster.')
            stringParam('EBS_ROOT_VOLUME_SIZE', env.get('EBS_ROOT_VOLUME_SIZE', allVars.get('EBS_ROOT_VOLUME_SIZE')), 'Size of the root volume for instances in the cluster.')
            stringParam('EMR_VOLUME_SIZE', env.get('EMR_VOLUME_SIZE', allVars.get('EMR_VOLUME_SIZE')), 'Size of the ebs volume for instances in the cluster.')
        }
    }
    // Include this whenever secure_scm() is used, or when run-automated-task.sh is executed in a shell command.
    public static def secure_scm_parameters = { allVars ->
        return {
            stringParam('SECURE_BRANCH', allVars.get('SECURE_BRANCH', '$ANALYTICS_SECURE_RELEASE'), 'e.g. tagname or origin/branchname, or $ANALYTICS_SECURE_RELEASE when released.')
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

    // Send email notifications on job failures.
    // * If you need to notify a predefined email address(es), you must either include `common_parameters()` or manually
    //   include a `NOTIFY` parameter.
    public static def common_publishers = { allVars ->
        return {
            flexiblePublish {
                // Optionally send emails to the addresses in the $NOTIFY parameter.
                conditionalAction {
                    condition {
                        not {
                            // method signature
                            // stringsMatch(String arg1, String arg2, boolean ignoreCase)
                            stringsMatch('$NOTIFY', '', false)
                        }
                    }
                    publishers {
                        mailer('$NOTIFY')
                    }
                }
            }
        }
    }

    public static def common_groovy_postbuild = { dslFactory, allVars ->
        return {
            groovyPostBuild {
                script(dslFactory.readFileFromWorkspace('dataeng/resources/set_build_status.groovy'))
            }
        }
    }

    public static def common_datadog_build_end = { dslFactory, allVars ->
        return {
            postBuildTask {
                task('Started', dslFactory.readFileFromWorkspace('dataeng/resources/datadog_job_end.sh'), false, false)
            }
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

    public static def common_authorization = { allVars ->
        return {
            allVars.get('USER_ROLES').each { github_id, roles ->
                roles.each {
                    role -> permission(role, github_id)
                }
            }
        }
    }

    public static def opsgenie_heartbeat_publisher = { allVars ->
        // No args - instead the method uses the following environment variables to keep them hidden:
        // OPSGENIE_HEARTBEAT_NAME:  Name of the OpsGenie heartbeat to disable.
        // OPSGENIE_HEARTBEAT_CONFIG_KEY: API key that authorizes Jenkins to OpsGenie so that heartbeats can be disabled.
        //
        // The task should *always* run on job completion, so match *any* text at all before running.
        return {
            postBuildTask {
                task('.*', 'if [ -n "$OPSGENIE_HEARTBEAT_NAME" ] && [ -n "$OPSGENIE_HEARTBEAT_CONFIG_KEY" ]; then curl -X POST "https://api.opsgenie.com/v2/heartbeats/$OPSGENIE_HEARTBEAT_NAME/disable" --header "Authorization: GenieKey $OPSGENIE_HEARTBEAT_CONFIG_KEY"; fi', true)
            }
        }
    }
    public static def slack_publisher = {
        return {
            slackNotifier {
                room('$SLACK_NOTIFICATION_CHANNEL')
                botUser (true)
                startNotification (false) // A build has started, we do not want such notifications
                notifySuccess (false) // A build was successfull, , we do not want such notifications
                notifyAborted (true)
                notifyNotBuilt (false)
                notifyUnstable (true)
                notifyFailure(true)
                notifyBackToNormal (true)
                notifyRepeatedFailure (true) // Notify on repeating failures
                matrixTriggerMode('ONLY_CONFIGURATIONS')
                commitInfoChoice('AUTHORS_AND_TITLES')
                customMessageFailure('$FAILURE_MESSAGE')
            }

        }
    }
}

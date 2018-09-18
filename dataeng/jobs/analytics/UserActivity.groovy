package analytics

class UserActivity {
    public static def job = { dslFactory, extraVars ->
        dslFactory.job('user-activity') {
            parameters {
                stringParam('CLUSTER_NAME', 'UserActivityTest', 'Name of the EMR cluster to provision for this job.')
                // EMR cluster configuration
                stringParam('EMR_MASTER_INSTANCE_TYPE', 'm4.2xlarge', 'EC2 Instance type used for master.')
                stringParam('EMR_MASTER_USE_SPOT_INSTANCE', 'False', 'Use spot instances.')
                stringParam('EMR_WORKER_INSTANCE_TYPE_1', 'm4.2xlarge', 'EC2 instance type used by workers.')
                stringParam('EMR_WORKER_INSTANCE_TYPE_2', 'm4.4xlarge', 'EC2 instance type used by workers.')
                stringParam('EMR_ADDITIONAL_APPLICATION_PROPERTIES', ' ', 'Additional configuration properties for applications. Use blank space as default value.')
                stringParam('EMR_MAPRED_SITE_PROPERTIES', ' ', 'Additional hadoop mapred-site properties. Use blank space as default value.')
                stringParam('EMR_YARN_SITE_PROPERTIES', ' ', 'Additional hadoop yarn-site properties. Use blank space as default value.')
                stringParam('EMR_USER_INFO', ' ', 'Additional github users with ssh access to cluster. Use blank space as default value.')
                // END EMR cluster configuration
                stringParam('TASK_USER', '$AUTOMATION_USER')
                stringParam('TASKS_BRANCH', '$ANALYTICS_PIPELINE_RELEASE', 'Branch from the edx-analytics-pipeline repository. For tags use tags/[tag-name].')
                stringParam('SECURE_REPO', extraVars.get('SECURE_REPO_URL'))
                stringParam('SECURE_BRANCH', '$ANALYTICS_SECURE_RELEASE', 'Branch from the analytics-secure repository, where the configuration settings reside. For tags use tags/[tag-name]')
                stringParam('SECURE_CONFIG', 'analytics-tasks/dev-edx.cfg', 'Config file from the analytics-secure repository for this job.')
                stringParam('CONF_BRANCH', '$ANALYTICS_CONFIGURATION_RELEASE', 'Branch from the edx-analytics-configuration repository. For tags use tags/[tag-name].')
                stringParam('NOTIFICATION_EMAILS', '$PAGER_NOTIFY', 'Space separated list of emails to notify in case of failure.')
                stringParam('TO_DATE', 'today', 'The day after the last date to export data for.')
                stringParam('NUM_TASK_CAPACITY', '8', 'Used to specify spot capacity of instance fleets core, also used to calculate the number of reducers by doubling this amount.')
                booleanParam('TERMINATE', true, 'Boolean indicating whether to terminate the cluster on job completion or not.')
                textParam('EXTRA_VARS', extraVars.get('AWS_EXTRA_VARS', ''))
            }

            multiscm{
                git {
                    remote {
                        url('git@github.com:edx/edx-analytics-pipeline.git')
                        branch('$TASKS_BRANCH')
                        credentials('1')
                    }
                    extensions {
                        pruneBranches()
                        relativeTargetDirectory('analytics-tasks')
                    }
                }
                git {
                    remote {
                        url('git@github.com:edx/edx-analytics-configuration.git')
                        branch('$CONF_BRANCH')
                        credentials('1')
                    }
                    extensions {
                        pruneBranches()
                        relativeTargetDirectory('analytics-configuration')
                    }
                }
            }

            wrappers {
                timestamps()
                sshAgent('1') // use credential's custom ID
            }

            steps {
                shell(dslFactory.readFileFromWorkspace("dataeng/resources/user-activity.sh"))
            }

            publishers {
                // Mark the build as 'unstable' if the text is found in 'console log'.
                textFinder("\\[Traceback\\]", '', true, false, true)
                mailer('$NOTIFICATION_EMAILS')
            }
        }
    }
}

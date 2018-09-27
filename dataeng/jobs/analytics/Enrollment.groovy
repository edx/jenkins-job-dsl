package analytics

class Enrollment {
    public static def job = { dslFactory, extraVars ->
        dslFactory.job('enrollment') {
            parameters {
                stringParam('CONF_BRANCH', '$ANALYTICS_CONFIGURATION_RELEASE', 'Branch from the edx-analytics-configuration repository. For tags use tags/[tag-name].')
                stringParam('CLUSTER_NAME', 'EnrollmentTest', 'Name of the EMR cluster to provision for this job.')
                stringParam('NUM_TASK_CAPACITY', '45', 'Used to specify spot capacity of instance fleets core, also used to calculate the number of reducers by doubling this amount.')
                stringParam('NUM_REDUCE_TASKS', '90', 'Number of reducers for mapreduce job.')
                booleanParam('TERMINATE', true, 'Boolean indicating whether to terminate the cluster on job completion or not.')
                stringParam('FROM_DATE', '2013-11-01', 'The first date to export data for. Data for this date and all days before the "TO_DATE" parameter will be included.')
                stringParam('TO_DATE', 'today', 'The day after the last date to export data for. Data from the "FROM_DATE" parameter to 11:59:59 on the date before this date will be included.')
                stringParam('TASK_USER', '$AUTOMATION_USER')
                stringParam('SECURE_REPO', extraVars.get('SECURE_REPO_URL'))
                stringParam('SECURE_BRANCH', '$ANALYTICS_SECURE_RELEASE', 'Branch from the analytics-secure repository, where the configuration settings reside. For tags use tags/[tag-name]')
                stringParam('SECURE_CONFIG', 'analytics-tasks/dev-edx.cfg', 'Config file from the analytics-secure repository for this job.')
                stringParam('NOTIFY', '$PAGER_NOTIFY', 'Space separated list of emails to notify in case of failure.')
                stringParam('EXTRA_ARGS', '')
                stringParam('TASKS_BRANCH', '$ANALYTICS_PIPELINE_RELEASE', 'Branch from the edx-analytics-pipeline repository. For tags use tags/[tag-name].')
                // EMR cluster configuration
                stringParam('EMR_MASTER_INSTANCE_TYPE', 'm4.2xlarge', 'EC2 Instance type used for master.')
                stringParam('EMR_MASTER_USE_SPOT_INSTANCE', 'False', 'Use spot instances.')
                stringParam('EMR_WORKER_INSTANCE_TYPE_1', 'm4.2xlarge', 'EC2 instance type used by workers.')
                stringParam('EMR_WORKER_INSTANCE_TYPE_2', 'm4.4xlarge', 'EC2 instance type used by workers.')
                stringParam('EMR_ADDITIONAL_APPLICATION_PROPERTIES', ' ', 'Additional configuration properties for applications. Use blank space as default value.')
                stringParam('EMR_MAPRED_SITE_PROPERTIES', ' ', 'Additional hadoop mapred-site properties. Use blank space as default value.')
                stringParam('EMR_YARN_SITE_PROPERTIES', ' ', 'Additional hadoop yarn-site properties. Use blank space as default value.')
                stringParam('EMR_USER_INFO', ' ', 'Additional github users with ssh access to cluster. Use blank space as default value.')
                textParam('EXTRA_VARS', extraVars.get('AWS_EXTRA_VARS', ''))
                // END EMR cluster configuration
            }

            logRotator {
                daysToKeep(365)
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
                shell(dslFactory.readFileFromWorkspace("dataeng/resources/enrollment.sh"))
            }

            publishers {
                // Mark the build as 'unstable' if the text is found in 'console log'.
                textFinder("\\[Traceback\\]", '', true, false, true)
                mailer('$NOTIFY')
            }
        }
    }
}

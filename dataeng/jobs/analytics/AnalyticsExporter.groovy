package analytics
import static org.edx.jenkins.dsl.AnalyticsConstants.common_publishers
import static org.edx.jenkins.dsl.AnalyticsConstants.secure_scm
import static org.edx.jenkins.dsl.AnalyticsConstants.secure_scm_parameters
import static org.edx.jenkins.dsl.AnalyticsConstants.opsgenie_heartbeat_publisher

class AnalyticsExporter {
    public static def job = { dslFactory, allVars ->
        dslFactory.job('analytics-exporter-course') {
            description('The course-level one-off version of the Analytics Exporter job.  Use this to export only a single course rather than a whole org.  Mainly for RDX purposes.')
            parameters {
                stringParam('COURSES', '', 'Space separated list of courses to process. E.g. --course=course-v1:BerkleeX+BMPR365_3x+1T2015')
                stringParam('EXPORTER_BRANCH', 'environment/production', 'Branch from the analytics-exporter repository. For tags use tags/[tag-name].')
                stringParam('PLATFORM_BRANCH', 'origin/release', 'Branch from the exporter repository. For tags use tags/[tag-name].')
                stringParam('EXPORTER_CONFIG_FILENAME', 'course_exporter.yaml', 'Name of configuration file in analytics-secure/analytics-exporter.')
                stringParam('OUTPUT_BUCKET', '', 'Name of the bucket for the destination of the export data. Can use a path. (eg. export-data/test).')
                stringParam('NOTIFY', '', 'Space separated list of emails to notify in case of failure.')
                stringParam('DATE_MODIFIER', '', 'Used to set the date of the CWSM dump.  Leave blank to use today\'s date.  Set to "-d 202x-0x-0x" if that is when the CWSM dump took place.  (Leave off quotes.)')
                stringParam('TASKS', '', 'Space separated list of tasks to process. Leave this blank to use the task list specified in the config file.  Specify here only if you are running tests of a specific task.')
            }
            parameters secure_scm_parameters(allVars)

            environmentVariables {
                env('REMOTE_CONFIG_PROD_EDX_ROLE_ARN', allVars.get('REMOTE_CONFIG_PROD_EDX_ROLE_ARN'))
                env('REMOTE_CONFIG_PROD_EDX_LMS', allVars.get('REMOTE_CONFIG_PROD_EDX_LMS'))
                env('REMOTE_CONFIG_PROD_EDX_STUDIO', allVars.get('REMOTE_CONFIG_PROD_EDX_STUDIO'))
                env('REMOTE_CONFIG_PROD_EDGE_ROLE_ARN', allVars.get('REMOTE_CONFIG_PROD_EDGE_ROLE_ARN'))
                env('REMOTE_CONFIG_PROD_EDGE_LMS', allVars.get('REMOTE_CONFIG_PROD_EDGE_LMS'))
                env('REMOTE_CONFIG_PROD_EDGE_STUDIO', allVars.get('REMOTE_CONFIG_PROD_EDGE_STUDIO'))
                env('REMOTE_CONFIG_DECRYPTION_KEYS_VAULT_KV_PATH', allVars.get('REMOTE_CONFIG_DECRYPTION_KEYS_VAULT_KV_PATH'))
                env('REMOTE_CONFIG_DECRYPTION_KEYS_VAULT_KV_VERSION', allVars.get('REMOTE_CONFIG_DECRYPTION_KEYS_VAULT_KV_VERSION'))
            }

            multiscm secure_scm(allVars) << {
                git {
                    remote {
                        url('git@github.com:edx/edx-platform.git')
                        branch('$PLATFORM_BRANCH')
                        credentials('1')
                    }
                    extensions {
                        pruneBranches()
                        relativeTargetDirectory('edx-platform')
                    }
                }
                git {
                    remote {
                        url('git@github.com:edx/edx-analytics-exporter.git')
                        branch('$EXPORTER_BRANCH')
                        credentials('1')
                    }
                    extensions {
                        pruneBranches()
                        relativeTargetDirectory('analytics-exporter')
                    }
                }

            }

            wrappers {
                timestamps()
                credentialsBinding {
                    usernamePassword('ANALYTICS_VAULT_ROLE_ID', 'ANALYTICS_VAULT_SECRET_ID', 'analytics-vault');
                }
            }

            steps {
                // This will create python 3.8 venv inside shell script instead of using shiningpanda
                shell(dslFactory.readFileFromWorkspace('dataeng/resources/setup-platform-venv-py3.sh'))
                virtualenv {
                    pythonName('PYTHON_3.7')
                    nature("shell")
                    command(
                        dslFactory.readFileFromWorkspace("dataeng/resources/remote-config.sh")
                    )
                }
                virtualenv {
                    // The exporter itself still runs python 2.
                    nature("shell")
                    name("analytics-exporter")
                    command(
                        dslFactory.readFileFromWorkspace("dataeng/resources/run-course-exporter.sh")
                    )
                }
            }
        }

        dslFactory.job('analytics-exporter-worker') {
            description('This is a worker/downstream job to the Analytics Exporter. It does all of the legwork of exporting/encrypting the data for a given org. See also: analytics-exporter-master.')
            parameters {
                stringParam('NOTIFY')
                stringParam('MASTER_WORKSPACE')
                stringParam('ORG_CONFIG_PATH')
                stringParam('GPG_KEYS_PATH')
                stringParam('DATE')
                stringParam('CONFIG_PATH')
                stringParam('OUTPUT_BUCKET')
                stringParam('EXPORTER_VENV')
                stringParam('ORG')
                stringParam('PLATFORM_VENV')
                stringParam('EXTRA_OPTIONS')
            }
            parameters secure_scm_parameters(allVars)

            environmentVariables {
                env('REMOTE_CONFIG_PROD_EDX_ROLE_ARN', allVars.get('REMOTE_CONFIG_PROD_EDX_ROLE_ARN'))
                env('REMOTE_CONFIG_PROD_EDX_LMS', allVars.get('REMOTE_CONFIG_PROD_EDX_LMS'))
                env('REMOTE_CONFIG_PROD_EDX_STUDIO', allVars.get('REMOTE_CONFIG_PROD_EDX_STUDIO'))
                env('REMOTE_CONFIG_PROD_EDGE_ROLE_ARN', allVars.get('REMOTE_CONFIG_PROD_EDGE_ROLE_ARN'))
                env('REMOTE_CONFIG_PROD_EDGE_LMS', allVars.get('REMOTE_CONFIG_PROD_EDGE_LMS'))
                env('REMOTE_CONFIG_PROD_EDGE_STUDIO', allVars.get('REMOTE_CONFIG_PROD_EDGE_STUDIO'))
                env('REMOTE_CONFIG_DECRYPTION_KEYS_VAULT_KV_PATH', allVars.get('REMOTE_CONFIG_DECRYPTION_KEYS_VAULT_KV_PATH'))
                env('REMOTE_CONFIG_DECRYPTION_KEYS_VAULT_KV_VERSION', allVars.get('REMOTE_CONFIG_DECRYPTION_KEYS_VAULT_KV_VERSION'))
            }

            logRotator {
                daysToKeep(30)
            }

            throttleConcurrentBuilds {
                maxPerNode(12)
                maxTotal(12)
            }

            concurrentBuild()

            multiscm secure_scm(allVars)

            wrappers {
                timestamps()
                buildName('#${BUILD_NUMBER} ${ENV,var="ORG"}')
                credentialsBinding {
                    usernamePassword('ANALYTICS_VAULT_ROLE_ID', 'ANALYTICS_VAULT_SECRET_ID', 'analytics-vault');
                }
            }

            steps {
                virtualenv {
                    pythonName('PYTHON_3.7')
                    nature("shell")
                    command(
                        dslFactory.readFileFromWorkspace("dataeng/resources/vault-config.sh")
                    )
                }
                virtualenv {
                    pythonName('PYTHON_3.7')
                    nature("shell")
                    command(
                        dslFactory.readFileFromWorkspace("dataeng/resources/remote-config.sh")
                    )
                }
                shell(dslFactory.readFileFromWorkspace("dataeng/resources/org-exporter-worker.sh"))
            }

            publishers {
                // Mark the build as 'unstable' if the text is found in 'console log'.
                textFinder("\\[WARNING\\]", '', true, false, true)
                // Cleanup the remote-config credentials.
                wsCleanup {
                    includePattern('remote-config/**')
                    deleteDirectories(true)
                }
            }
        }

        dslFactory.job('analytics-exporter-master') {
            description('The Analytics Exporter weekly job, which exports tons of structure and state data for every course for every participating org and delivers them encrypted to our partners via S3.  Specifically, this sets up the shared edx-platform execution environment, fetches a list of all the orgs, then kicks off downstream analytics-exporter-worker jobs for each one that corresponds to a partner which is configured to receive export data.')
            parameters {
                stringParam('ORGS', '*', 'Space separated list of organizations to process. Can use wildcards. e.g.: idbx HarvardX')
                stringParam('EXPORTER_BRANCH', 'environment/production', 'Branch from the edx-analytics-exporter repository. For tags use tags/[tag-name].')
                stringParam('PLATFORM_BRANCH', 'origin/release', 'Branch from the edx-platform repository. For tags use tags/[tag-name].')
                stringParam('EXPORTER_CONFIG_FILENAME', 'default.yaml', 'Name of configuration file in analytics-secure/analytics-exporter.')
                stringParam('OUTPUT_BUCKET', allVars.get('EXPORTER_OUTPUT_BUCKET'), 'Name of the bucket for the destination of the export data. Can use a path. (eg. export-data/test).')
                stringParam('NOTIFY', allVars.get('ANALYTICS_EXPORTER_NOTIFY_LIST'), 'Space separated list of emails to notify in case of failure.')
                stringParam('DATE_MODIFIER', '', 'Used to set the date of the CWSM dump.  Leave blank to use today\'s date.  Set to "-d 202x-0x-0x" if that is when the CWSM dump took place.  (Leave off quotes.)')
                stringParam('EXTRA_OPTIONS', '--exclude-task=OrgEmailOptInTask', 'e.g. --exclude-task=OrgEmailOptInTask')
                stringParam('ORG_CONFIG', 'data-czar-keys/config.yaml', 'Path to the data-czar organization config file.')
                stringParam('DATA_CZAR_KEYS_BRANCH', 'master', 'Branch to use for the data-czar-keys repository.')
                stringParam('PRIORITY_ORGS', allVars.get('PRIORITY_ORGS'), 'Space separated list of organizations to process first.')
                stringParam('JOB_DSL_BRANCH', 'origin/master', 'Branch from the jenkins job dsl repository to get vault token helper.')
            }
            parameters secure_scm_parameters(allVars)
            environmentVariables {
                env('OPSGENIE_HEARTBEAT_NAME', allVars.get('OPSGENIE_HEARTBEAT_NAME'))
                env('OPSGENIE_HEARTBEAT_DURATION_NUM', allVars.get('OPSGENIE_HEARTBEAT_DURATION_NUM'))
                env('OPSGENIE_HEARTBEAT_DURATION_UNIT', allVars.get('OPSGENIE_HEARTBEAT_DURATION_UNIT'))
            }

            multiscm secure_scm(allVars) << {
                git {
                    remote {
                        url('git@github.com:edx/edx-platform.git')
                        branch('$PLATFORM_BRANCH')
                        credentials('1')
                    }
                    extensions {
                        pruneBranches()
                        relativeTargetDirectory('edx-platform')
                    }
                }
                git {
                    remote {
                        url('git@github.com:edx/edx-analytics-exporter.git')
                        branch('$EXPORTER_BRANCH')
                        credentials('1')
                    }
                    extensions {
                        pruneBranches()
                        relativeTargetDirectory('analytics-exporter')
                    }
                }
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

            triggers{
                // Sundays around 10 a.m. UTC
                cron('H 10 * * 0')
            }

            wrappers {
                timestamps()
            }
            wrappers {
                credentialsBinding {
                    string('OPSGENIE_HEARTBEAT_CONFIG_KEY', 'opsgenie_heartbeat_config_key')
                }
            }

            publishers common_publishers(allVars)
            publishers opsgenie_heartbeat_publisher(allVars)

            steps {
                shell(dslFactory.readFileFromWorkspace('dataeng/resources/opsgenie-enable-heartbeat.sh'))
                // This will create python 3.8 venv inside shell script instead of using shiningpanda
                shell(dslFactory.readFileFromWorkspace('dataeng/resources/setup-platform-venv-py3.sh'))
                virtualenv {
                    // The exporter itself still runs python 2.
                    nature("shell")
                    name("analytics-exporter")
                    command(
                        dslFactory.readFileFromWorkspace("dataeng/resources/setup-exporter.sh")
                    )
                }

                downstreamParameterized {
                    trigger('analytics-exporter-worker') {
                        block {
                            // Mark this build step as FAILURE if at least one of the downstream builds were marked FAILED.
                            buildStepFailure('FAILURE')
                            // Mark this entire build as FAILURE if at least one of the downstream builds were marked FAILED.
                            failure('FAILURE')
                            // Mark this entire build as UNSTABLE if at least one of the downstream builds were marked UNSTABLE.
                            unstable('UNSTABLE')
                        }
                        parameters {
                            predefinedProp('MASTER_WORKSPACE', '${WORKSPACE}')
                            predefinedProp('NOTIFY', '${NOTIFY}')
                        }
                        parameterFactories {
                            fileBuildParameterFactory {
                               filePattern('organizations/*')
                               encoding('UTF-8')
                               noFilesFoundAction('SKIP')
                            }
                        }
                    }
                }
            }
        }
    }
}

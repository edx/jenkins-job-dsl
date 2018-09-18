package analytics

class AnalyticsExporter {
    public static def job = { dslFactory, allVars ->
        dslFactory.job('analytics-exporter-course') {
            parameters {
                stringParam('COURSES', '', 'Space separated list of courses to process. E.g. --course=course-v1:BerkleeX+BMPR365_3x+1T2015')
                stringParam('EXPORTER_BRANCH', 'environment/production', 'Branch from the analytics-exporter repository. For tags use tags/[tag-name].')
                stringParam('PLATFORM_BRANCH', 'origin/zafft/analytics-exporter-settings-hotfix', 'Branch from the exporter repository. For tags use tags/[tag-name].')
                stringParam('SECURE_BRANCH', 'release', 'Branch from the analytics-secure repository, where the configuration settings reside. For tags use tags/[tag-name]')
                stringParam('CONFIG_FILENAME', 'course_exporter.yaml', 'Name of configuration file in analytics-secure/analytics-exporter.')
                stringParam('OUTPUT_BUCKET', '', 'Name of the bucket for the destination of the export data. Can use a path. (eg. export-data/test).')
                stringParam('NOTIFICATION_EMAILS', '', 'Space separated list of emails to notify in case of failure.')
                stringParam('DATE_MODIFIER', '', 'Used to set the date of the CWSM dump.  Leave blank to use today\'s date.  Set to "-d 201x-0x-0x" if that is when the CWSM dump took place.  (Leave off quotes.)')
                stringParam('TASKS', '', 'Space separated list of tasks to process. Leave this blank to use the task list specified in the config file.  Specify here only if you are running tests of a specific task.')
            }

            multiscm{
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
                        url(allVars.get('SECURE_REPO_URL'))
                        branch('$SECURE_BRANCH')
                        credentials('1')
                    }
                    extensions {
                        pruneBranches()
                        relativeTargetDirectory('analytics-secure')
                    }
                }
                git {
                    remote {
                        url(allVars.get('BAKED_CONFIG_SECURE_REPO_URL'))
                        branch('*/master')
                        credentials('1')
                    }
                    extensions {
                        relativeTargetDirectory('config/baked-config-secure')
                    }
                }
            }

            wrappers {
                timestamps()
            }

            steps {
                virtualenv {
                    nature("shell")
                    command(
                        dslFactory.readFileFromWorkspace("dataeng/resources/setup-platform-venv.sh")
                    )
                }
                virtualenv {
                    nature("shell")
                    name("analytics-exporter")
                    command(
                        dslFactory.readFileFromWorkspace("dataeng/resources/run-course-exporter.sh")
                    )
                }
            }
        }

        dslFactory.job('analytics-exporter-worker') {
            parameters {
                stringParam('NOTIFICATION_EMAILS')
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
                stringParam('SECURE_BRANCH')
            }

            logRotator {
                daysToKeep(30)
            }

            throttleConcurrentBuilds {
                maxPerNode(4)
                maxTotal(4)
            }

            concurrentBuild()

            multiscm {
                git {
                    remote {
                        url(allVars.get('BAKED_CONFIG_SECURE_REPO_URL'))
                        branch('*/master')
                        credentials('1')
                    }
                    extensions {
                        relativeTargetDirectory('config/baked-config-secure')
                    }
                }
                git {
                    remote {
                        url(allVars.get('SECURE_REPO_URL'))
                        branch('$SECURE_BRANCH')
                        credentials('1')
                    }
                    extensions {
                        pruneBranches()
                        relativeTargetDirectory('analytics-secure')
                    }
                }
            }

            wrappers {
                timestamps()
                buildName('#${BUILD_NUMBER} ${ENV,var="ORG"}')
            }

            steps {
                shell(dslFactory.readFileFromWorkspace("dataeng/resources/org-exporter-worker.sh"))
            }

            publishers {
                // Mark the build as 'unstable' if the text is found in 'console log'.
                textFinder("\\[WARNING\\]", '', true, false, true)
            }
        }

        dslFactory.job('analytics-exporter-master') {
            parameters {
                stringParam('ORGS', '*', 'Space separated list of organizations to process. Can use wildcards. e.g.: idbx HarvardX')
                stringParam('EXPORTER_BRANCH', 'environment/production', 'Branch from the edx-analytics-exporter repository. For tags use tags/[tag-name].')
                stringParam('PLATFORM_BRANCH', 'aed/analytics-exporter-settings-hotfix', 'Branch from the edx-platform repository. For tags use tags/[tag-name].')
                stringParam('SECURE_BRANCH', 'release', 'Branch from the analytics-secure repository, where the configuration settings reside. For tags use tags/[tag-name]')
                stringParam('CONFIG_FILENAME', 'default.yaml', 'Name of configuration file in analytics-secure/analytics-exporter.')
                stringParam('OUTPUT_BUCKET', allVars.get('EXPORTER_OUTPUT_BUCKET'), 'Name of the bucket for the destination of the export data. Can use a path. (eg. export-data/test).')
                stringParam('NOTIFICATION_EMAILS', allVars.get('ANALYTICS_EXPORTER_NOTIFY_LIST'), 'Space separated list of emails to notify in case of failure.')
                stringParam('DATE_MODIFIER', '', 'Used to set the date of the CWSM dump.  Leave blank to use today\'s date.  Set to "-d 201x-0x-0x" if that is when the CWSM dump took place.  (Leave off quotes.)')
                stringParam('EXTRA_OPTIONS', '--exclude-task=OrgEmailOptInTask', 'e.g. --exclude-task=OrgEmailOptInTask')
                stringParam('ORG_CONFIG', 'data-czar-keys/config.yaml', 'Path to the data-czar organization config file.')
                stringParam('DATA_CZAR_KEYS_BRANCH', 'master', 'Branch to use for the data-czar-keys repository.')
            }

            multiscm{
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
                        url(allVars.get('SECURE_REPO_URL'))
                        branch('$SECURE_BRANCH')
                        credentials('1')
                    }
                    extensions {
                        pruneBranches()
                        relativeTargetDirectory('analytics-secure')
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
                cron('# Sundays around 10 a.m. UTC\nH 10 * * 0')
            }

            wrappers {
                timestamps()
            }

            steps {
                virtualenv {
                    nature("shell")
                    command(
                        dslFactory.readFileFromWorkspace("dataeng/resources/setup-platform-venv-legacy.sh")
                    )
                }
                virtualenv {
                    nature("shell")
                    name("analytics-exporter")
                    command(
                        dslFactory.readFileFromWorkspace("dataeng/resources/setup-exporter.sh")
                    )
                }

                downstreamParameterized {
                    trigger('analytics-exporter-worker') {
                        block {
                            buildStepFailure('FAILURE')
                            failure('FAILURE')
                            unstable('UNSTABLE')
                        }
                        parameters {
                            predefinedProp('MASTER_WORKSPACE', '${WORKSPACE}')
                            predefinedProp('NOTIFICATION_EMAILS', '${NOTIFICATION_EMAILS}')
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

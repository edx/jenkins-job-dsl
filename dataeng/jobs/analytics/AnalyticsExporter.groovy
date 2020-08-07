package analytics
import static org.edx.jenkins.dsl.AnalyticsConstants.common_publishers
import static org.edx.jenkins.dsl.AnalyticsConstants.secure_scm
import static org.edx.jenkins.dsl.AnalyticsConstants.secure_scm_parameters

class AnalyticsExporter {
    public static def job = { dslFactory, allVars ->
        dslFactory.job('analytics-exporter-course') {
            parameters {
                stringParam('COURSES', '', 'Space separated list of courses to process. E.g. --course=course-v1:BerkleeX+BMPR365_3x+1T2015')
                stringParam('EXPORTER_BRANCH', 'environment/production', 'Branch from the analytics-exporter repository. For tags use tags/[tag-name].')
                stringParam('PLATFORM_BRANCH', 'origin/zafft/analytics-exporter-settings-hotfix', 'Branch from the exporter repository. For tags use tags/[tag-name].')
                stringParam('CONFIG_FILENAME', 'course_exporter.yaml', 'Name of configuration file in analytics-secure/analytics-exporter.')
                stringParam('OUTPUT_BUCKET', '', 'Name of the bucket for the destination of the export data. Can use a path. (eg. export-data/test).')
                stringParam('NOTIFY', '', 'Space separated list of emails to notify in case of failure.')
                stringParam('DATE_MODIFIER', '', 'Used to set the date of the CWSM dump.  Leave blank to use today\'s date.  Set to "-d 202x-0x-0x" if that is when the CWSM dump took place.  (Leave off quotes.)')
                stringParam('TASKS', '', 'Space separated list of tasks to process. Leave this blank to use the task list specified in the config file.  Specify here only if you are running tests of a specific task.')
            }
            parameters secure_scm_parameters(allVars)

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
            logRotator {
                daysToKeep(30)
            }

            throttleConcurrentBuilds {
                maxPerNode(6)
                maxTotal(6)
            }

            concurrentBuild()

            multiscm secure_scm(allVars) << {
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
                stringParam('CONFIG_FILENAME', 'default.yaml', 'Name of configuration file in analytics-secure/analytics-exporter.')
                stringParam('OUTPUT_BUCKET', allVars.get('EXPORTER_OUTPUT_BUCKET'), 'Name of the bucket for the destination of the export data. Can use a path. (eg. export-data/test).')
                stringParam('NOTIFY', allVars.get('ANALYTICS_EXPORTER_NOTIFY_LIST'), 'Space separated list of emails to notify in case of failure.')
                stringParam('DATE_MODIFIER', '', 'Used to set the date of the CWSM dump.  Leave blank to use today\'s date.  Set to "-d 202x-0x-0x" if that is when the CWSM dump took place.  (Leave off quotes.)')
                stringParam('EXTRA_OPTIONS', '--exclude-task=OrgEmailOptInTask', 'e.g. --exclude-task=OrgEmailOptInTask')
                stringParam('ORG_CONFIG', 'data-czar-keys/config.yaml', 'Path to the data-czar organization config file.')
                stringParam('DATA_CZAR_KEYS_BRANCH', 'master', 'Branch to use for the data-czar-keys repository.')
            }
            parameters secure_scm_parameters(allVars)

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
                cron('# Sundays around 10 a.m. UTC\nH 10 * * 0')
            }

            wrappers {
                timestamps()
            }

            publishers common_publishers(allVars)

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

package analytics
import static org.edx.jenkins.dsl.AnalyticsConstants.common_publishers
import static org.edx.jenkins.dsl.AnalyticsConstants.secure_scm
import static org.edx.jenkins.dsl.AnalyticsConstants.secure_scm_parameters

class AnalyticsEmailOptin {
    public static def job = { dslFactory, allVars ->
        dslFactory.job('analytics-email-optin-worker') {
            description('A version of the Analytics Exporter job that only runs the OrgEmailOptInTask task.')
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
                stringParam('OUTPUT_PREFIX')
                stringParam('PLATFORM_VENV')
            }

            logRotator {
                daysToKeep(30)
            }

            throttleConcurrentBuilds {
                maxPerNode(5)
                maxTotal(5)
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
            }

            wrappers {
                timestamps()
                buildName('#${BUILD_NUMBER} ${ENV,var="ORG"}')
            }

            steps {
                shell(dslFactory.readFileFromWorkspace("dataeng/resources/email-optin-worker.sh"))
            }

            publishers {
                textFinder("Task OrgEmailOptInTask failed fatally", '', true, false, false)
            }
        }
        dslFactory.job('analytics-email-optin-master') {
            parameters{
                stringParam('ORGS','*', 'Space separated list of organizations to process. Can use wildcards. e.g.: idbx HarvardX')
                stringParam('EXPORTER_BRANCH','environment/production',
                        'Branch from the edx-analytics-exporter repository. For tags use tags/[tag-name]. Should be environment/production.')

                // Temporarily use a hash rather than a release tag because at the time of this DSL change the edxapp
                // pipeline was stalled and could not create a release tag. The hash below IS ON MASTER and it
                // corresponds to this PR: https://github.com/edx/edx-platform/pull/25011
                //
                // TODO: next week we should fetch the latest release tag and update that here and also in
                // AnalyticsExporter.groovy.
                //
                //stringParam('PLATFORM_BRANCH','tags/release-2020-09-17-15.06', 'Branch from the edx-platform repository. For tags use tags/[tag-name]')
                stringParam('PLATFORM_BRANCH','b111d05149945634ce60daccd984801a852a9d13', 'Branch from the edx-platform repository. For tags use tags/[tag-name]')

                stringParam('EXPORTER_CONFIG_FILENAME','default.yaml', 'Name of configuration file in analytics-secure/analytics-exporter.')
                stringParam('OUTPUT_BUCKET', allVars.get('EMAIL_OPTIN_OUTPUT_BUCKET'), 'Name of the bucket for the destination of the email opt-in data.')
                stringParam('OUTPUT_PREFIX','email-opt-in-', 'Optional prefix to prepend to output filename.')
                stringParam('NOTIFY', allVars.get('ANALYTICS_EXPORTER_NOTIFY_LIST'),
                        'Space separated list of emails to notify in case of failure.')
                stringParam('DATE_MODIFIER','',
                        'Used to set the date of the CWSM dump.  Leave blank to use today\'s date.  Set to "-d 202x-0x-0x" if that is when the CWSM dump took place, typically the preceding Sunday.  (Leave off quotes.)')
                stringParam('ORG_CONFIG','data-czar-keys/config.yaml', 'Path to the data-czar organization config file')
                stringParam('DATA_CZAR_KEYS_BRANCH','master', 'Branch of the Data-czar-keys repository to use')
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
                // Saturdays around 4 a.m. UTC
                cron('H 4 * * 6')
            }

            wrappers {
                timestamps()
            }

            publishers common_publishers(allVars)

            steps {
                virtualenv {
                    pythonName('PYTHON_3.7')
                    nature("shell")
                    command(
                        dslFactory.readFileFromWorkspace("dataeng/resources/setup-platform-venv-py3.sh")
                    )
                }
                virtualenv {
                    // The exporter itself still runs python 2.
                    nature("shell")
                    name("analytics-exporter")
                    command(
                        dslFactory.readFileFromWorkspace("dataeng/resources/setup-exporter-email-optin.sh")
                    )
                }
                downstreamParameterized {
                    trigger('analytics-email-optin-worker') {
                        block {
                            buildStepFailure('FAILURE')
                            failure('FAILURE')
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


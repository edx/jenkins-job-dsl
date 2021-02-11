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
                maxPerNode(5)
                maxTotal(5)
            }

            concurrentBuild()

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
                        dslFactory.readFileFromWorkspace("dataeng/resources/remote-config.sh")
                    )
                }
                shell(dslFactory.readFileFromWorkspace("dataeng/resources/email-optin-worker.sh"))
            }

            publishers {
                textFinder("Task OrgEmailOptInTask failed fatally", '', true, false, false)
                // Cleanup the remote-config credentials.
                wsCleanup {
                    includePattern('remote-config/**')
                    deleteDirectories(true)
                }
            }
        }
        dslFactory.job('analytics-email-optin-master') {
            parameters{
                stringParam('ORGS','*', 'Space separated list of organizations to process. Can use wildcards. e.g.: idbx HarvardX')
                stringParam('EXPORTER_BRANCH','environment/production',
                        'Branch from the edx-analytics-exporter repository. For tags use tags/[tag-name]. Should be environment/production.')
                stringParam('PLATFORM_BRANCH', 'origin/release', 'Branch from the edx-platform repository. For tags use tags/[tag-name]')
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
                // This will create python 3.8 venv inside shell script instead of using shiningpanda
                shell(dslFactory.readFileFromWorkspace('platform/resources/setup-platform-venv-py3.sh'))
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


import org.yaml.snakeyaml.Yaml
import org.yaml.snakeyaml.error.YAMLException


Map config = [:]
Binding bindings = getBinding()
config.putAll(bindings.getVariables())
PrintStream out = config['out']

Map globals = binding.variables
String commonVarsDir = globals.get('COMMON_VARS_DIR')
String commonVarsFilePath = commonVarsDir + 'common.yaml'
Map commonConfigMap = [:]

try {
    out.println('Parsing secret YAML file')
    String commonConfigContents = readFileFromWorkspace(commonVarsFilePath)
    Yaml yaml = new Yaml()
    commonConfigMap = yaml.load(commonConfigContents)
    out.println('Successfully parsed secret YAML file')

}  catch (YAMLException e) {
  throw new IllegalArgumentException("Unable to parse ${commonVarsFilePath}: ${e.message}")
}


job ('analytics-email-optin-worker') {

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
        stringParam('OUTPUT_PREFIX')
        stringParam('PLATFORM_VENV')
    }

    throttleConcurrentBuilds {
        maxPerNode(5)
        maxTotal(5)
    }

    concurrentBuild()

    multiscm {
        git {
            remote {
                url(commonConfigMap.get('BAKED_CONFIG_SECURE_REPO_URL'))
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
        shell(readFileFromWorkspace("dataeng/resources/email-optin-worker.sh"))
    }

    publishers {
        textFinder("Task OrgEmailOptInTask failed fatally", '', true, false, false)
    }
}

job ('analytics-email-optin-master') {

    parameters{
        stringParam('ORGS','*', 'Space separated list of organizations to process. Can use wildcards. e.g.: idbx HarvardX')
        stringParam('EXPORTER_BRANCH','environment/production',
                'Branch from the edx-analytics-exporter repository. For tags use tags/[tag-name]. Should be environment/production.')
        stringParam('PLATFORM_BRANCH','zafft/analytics-exporter-settings-hotfix',
                'Branch from the edx-platform repository. For tags use tags/[tag-name]. Should be release.')
        stringParam('SECURE_BRANCH','release',
                'Branch from the analytics-secure repository, where the configuration settings reside. For tags use tags/[tag-name]')
        stringParam('CONFIG_FILENAME','default.yaml', 'Name of configuration file in analytics-secure/analytics-exporter.')
        stringParam('OUTPUT_BUCKET', commonConfigMap.get('EMAIL_OPTIN_OUTPUT_BUCKET'), 'Name of the bucket for the destination of the email opt-in data.')
        stringParam('OUTPUT_PREFIX','email-opt-in-', 'Optional prefix to prepend to output filename.')
        stringParam('NOTIFICATION_EMAILS', commonConfigMap.get('EXTENDED_NOTIFY_LIST'),
                'Space separated list of emails to notify in case of failure.')
        stringParam('DATE_MODIFIER','',
                'Used to set the date of the CWSM dump.  Leave blank to use today\'s date.  Set to "-d 201x-0x-0x" if that is when the CWSM dump took place, typically the preceding Sunday.  (Leave off quotes.)')
        stringParam('ORG_CONFIG','data-czar-keys/config.yaml', 'Path to the data-czar organization config file')
        stringParam('DATA_CZAR_KEYS_BRANCH','master', 'Branch of the Data-czar-keys repository to use')
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
                url(commonConfigMap.get('SECURE_REPO_URL'))
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
                url(commonConfigMap.get('DATA_CZAR_KEYS_REPO_URL'))
                branch('$DATA_CZAR_KEYS_BRANCH')
                credentials('1')
            }
            extensions {
                relativeTargetDirectory('data-czar-keys')
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
                readFileFromWorkspace("dataeng/resources/setup-platform-venv.sh")
            )
        }
        virtualenv {
            nature("shell")
            command(
                readFileFromWorkspace("dataeng/resources/setup-exporter-email-optin.sh")
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

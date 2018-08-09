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

job ('analytics-exporter-course') {
    parameters {
        stringParam('COURSES', '', 'Space separated list of courses to process. E.g. --course=course-v1:BerkleeX+BMPR365_3x+1T2015')
        stringParam('EXPORTER_BRANCH', 'environment/production', 'Branch from the analytics-exporter repository. For tags use tags/[tag-name].')
        stringParam('PLATFORM_BRANCH', 'release', 'Branch from the exporter repository. For tags use tags/[tag-name].')
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
            name("analytics-exporter")
            command(
                readFileFromWorkspace("dataeng/resources/run-course-exporter.sh")
            )
        }
    }
}

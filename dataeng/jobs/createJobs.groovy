import static analytics.AnalyticsEmailOptin.job as AnalyticsEmailOptinJob
import static analytics.AnalyticsExporter.job as AnalyticsExporterJob
import static analytics.UserActivity.job as UserActivityJob
import org.yaml.snakeyaml.Yaml
import org.yaml.snakeyaml.error.YAMLException

def globals = binding.variables
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

def taskList = [
    UserActivityJob
]

for (task in taskList) {
    task(this, commonConfigMap)
}

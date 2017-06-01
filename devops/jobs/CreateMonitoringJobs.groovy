/*
  DSL used by the devops seed job to create some devops jobs (can later be refactored
  to be a list, for now, you just put a createJobs.groovy in the "Server" named directory,
  and load the jobs/devops/*.groovy files you want in as below.
  We also expect an EXTRA_VARS variable from the seed job (usually a Text input) and you
  can either provide @path/to/foo.yaml checked out from a secure repo (suggested path) or
  input yaml in the text area to be parsed.  These variables are parsed and passed down to
  the jobs created.
  We expect these basic vars and jobs may expect additional vars.  Consult job docs for those.
  Variables without defaults are marked (required).  Some of these variables are consumed by
  the common_* functions available in DevopsTasks.groovy
  * CONFIGURATION_SECURE_REPO: git@github.com:your/secure.git
  * CONFIGURATION_SECURE_BRANCH: origin/master (required)
  * CONFIGURATION_BRANCH: origin/master
  * CONFIGURATION_REPO: git@github.com:edx/configuration.git
  * AWS_REGION: us-east-1
  * SECURE_GIT_CREDENTIALS: secure-bot-user (required)
  * DEPLOYMENTS: (required)
      edx:
        environments:
          -  loadtest
          -  prod
          -  stage
      edge:
        environments:
          -  prod
      mckinsey:
        environments:
          -  stage
          -  prod
        cluster_name: commoncluster
        sudo_user: root
  * PLAYS:        (required)
        - edxapp
    
  * NOTIFY_ON_FAILURE: alert@example.com
  * FOLDER_NAME: folder
 */
import org.edx.jenkins.dsl.DevopsTasks
import static org.edx.jenkins.dsl.YAMLHelpers.parseYaml
import org.yaml.snakeyaml.error.YAMLException
import static devops.BuildAMI.job as BuildAMIJob
import static devops.CheckRabbit.job as CheckRabbitJob
import static devops.MinosLifecycle.job as MinosLifecycleJob

Map globals = binding.variables
String extraVarsStr = globals.get('EXTRA_VARS')
Map extraVars = [:]
if (extraVarsStr) {
    try {
      extraVars = parseYaml(extraVarsStr, this)
    } catch (YAMLException e) {
      throw new IllegalArgumentException("Unable to parse ${extraVarsKey}: ${e.message}")
    }
}


CheckRabbitJob(this, globals + extraVars)
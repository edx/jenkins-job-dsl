package platform

import org.yaml.snakeyaml.Yaml
import static org.edx.jenkins.dsl.JenkinsPublicConstants.JENKINS_PUBLIC_TEAM_SECURITY
import static org.edx.jenkins.dsl.JenkinsPublicConstants.JENKINS_PUBLIC_LOG_ROTATOR

/* stdout logger */
Map config = [:]
Binding bindings = getBinding()
config.putAll(bindings.getVariables())
PrintStream out = config['out']

/* Map to hold the k:v pairs parsed from the secret file */
Map ghprbMap = [:]
try {
    out.println('Parsing secret YAML file')
    String ghprbConfigContents = new File("${GHPRB_SECRET}").text
    Yaml yaml = new Yaml()
    ghprbMap = yaml.load(ghprbConfigContents)
    out.println('Successfully parsed secret YAML file')
}
catch (any) {
    out.println('Jenkins DSL: Error parsing secret YAML file')
    out.println('Exiting with error code 1')
    return 1
}

// This is the job DSL responsible for creating the main pipeline job.
pipelineJob('edx-platform-python-pipeline-pr') {

    definition {

        triggers {
            pullRequest {
                admins(ghprbMap['admin'])
                useGitHubHooks()
                triggerPhrase(/.*jenkins\W+run\W+pipeline\W+python.*/)
                onlyTriggerPhrase(true)
                userWhitelist(ghprbMap['userWhiteList'])
                orgWhitelist(ghprbMap['orgWhiteList'])
                extensions {
                    commitStatus {
                        context('jenkins/python-pipeline')
                    }
                }
            }
        }

        cpsScm {
            scm {
                git {
                    remote {
                        credentials('jenkins-worker')
                        github('edx/edx-platform', 'ssh', 'github.com')
                        refspec('+refs/heads/*:refs/remotes/origin/* +refs/pull/*:refs/remotes/origin/pr/*')
                        branch('\${sha1}')
                    }
                }
            }
            scriptPath('Jenkinsfile')
        }
    }

    configure {
        it / definition / lightweight(true)
    }
}

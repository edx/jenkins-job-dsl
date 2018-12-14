package platform

import org.yaml.snakeyaml.Yaml
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
pipelineJob('edx-platform-lettuce-pipeline-pr') {

    definition {

        logRotator JENKINS_PUBLIC_LOG_ROTATOR(7)

        triggers {
            githubPullRequest {
                admins(ghprbMap['admin'])
                useGitHubHooks()
                triggerPhrase(/.*jenkins\W+run\W+pipeline\W+lettuce.*/)
                onlyTriggerPhrase(true)
                userWhitelist(ghprbMap['userWhiteList'])
                orgWhitelist(ghprbMap['orgWhiteList'])
                extensions {
                    commitStatus {
                        context('jenkins/lettuce-pipeline')
                    }
                }
            }
        }

        cpsScm {
            scm {
                git {
                    extensions {
                        cloneOptions {
                            honorRefspec(true)
                            noTags(true)
                            shallow(true)
                        }
                        sparseCheckoutPaths {
                            sparseCheckoutPaths {
                                sparseCheckoutPath {
                                    path('scripts/Jenkinsfiles')
                                }
                            }
                        }
                    }
                    remote {
                        credentials('jenkins-worker')
                        github('edx/edx-platform', 'ssh', 'github.com')
                        refspec('+refs/heads/master:refs/remotes/origin/master +refs/pull/${ghprbPullId}/*:refs/remotes/origin/pr/${ghprbPullId}/*')
                        branch('\${sha1}')
                    }
                }
            }
            scriptPath('scripts/Jenkinsfiles/lettuce')
        }
    }
}

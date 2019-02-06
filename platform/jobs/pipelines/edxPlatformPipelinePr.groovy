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

Map publicBokchoyJobConfig = [
    jobName: 'edx-platform-bokchoy-pipeline-pr',
    context: 'jenkins/bokchoy',
    onlyTriggerPhrase: true,
    triggerPhrase: /.*jenkins\W+run\W+pipeline\W+bokchoy.*/,
    jenkinsFileDir: 'scripts/Jenkinsfiles',
    jenkinsFileName: 'bokchoy'
]

Map publicLettuceJobConfig = [
    jobName: 'edx-platform-lettuce-pipeline-pr',
    context: 'jenkins/lettuce',
    onlyTriggerPhrase: true,
    triggerPhrase: /.*jenkins\W+run\W+pipeline\W+lettuce.*/,
    jenkinsFileDir: 'scripts/Jenkinsfiles',
    jenkinsFileName: 'lettuce'
]

Map publicPythonJobConfig = [
    jobName: 'edx-platform-python-pipeline-pr',
    context: 'jenkins/python',
    onlyTriggerPhrase: true,
    triggerPhrase: /.*jenkins\W+run\W+pipeline\W+python.*/,
    jenkinsFileDir: 'scripts/Jenkinsfiles',
    jenkinsFileName: 'python'
]

Map publicQualityJobConfig = [
    jobName: 'edx-platform-quality-pipeline-pr',
    context: 'jenkins/quality',
    onlyTriggerPhrase: true,
    triggerPhrase: /.*jenkins\W+run\W+pipeline\W+quality.*/,
    jenkinsFileDir: 'scripts/Jenkinsfiles',
    jenkinsFileName: 'quality'
]

List jobConfigs = [
    publicBokchoyJobConfig,
    publicLettuceJobConfig,
    publicPythonJobConfig,
    publicQualityJobConfig
]

/* Iterate over the job configurations */
jobConfigs.each { jobConfig ->

    // This is the job DSL responsible for creating the main pipeline job.
    pipelineJob(jobConfig.jobName) {

        definition {

            logRotator JENKINS_PUBLIC_LOG_ROTATOR(7)

            triggers {
                githubPullRequest {
                    admins(ghprbMap['admin'])
                    useGitHubHooks()
                    triggerPhrase(jobConfig.triggerPhrase)
                    onlyTriggerPhrase(jobConfig.onlyTriggerPhrase)
                    userWhitelist(ghprbMap['userWhiteList'])
                    orgWhitelist(ghprbMap['orgWhiteList'])
                    extensions {
                        commitStatus {
                            context(jobConfig.context)
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
                                        path(jobConfig.jenkinsFileDir)
                                    }
                                }
                            }
                        }
                        remote {
                            credentials('jenkins-worker')
                            github('raccoongang/edx-platform', 'ssh', 'github.com')
                            refspec('+refs/pull/${ghprbPullId}/*:refs/remotes/origin/pr/${ghprbPullId}/*')
                            branch('\${sha1}')
                        }
                    }
                }
                scriptPath(jobConfig.jenkinsFileDir + '/' + jobConfig.jenkinsFileName)
            }
        }
    }
}

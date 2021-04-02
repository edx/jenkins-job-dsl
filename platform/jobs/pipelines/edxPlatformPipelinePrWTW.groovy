package platform

import org.yaml.snakeyaml.Yaml
import static org.edx.jenkins.dsl.JenkinsPublicConstants.GENERAL_PRIVATE_JOB_SECURITY
import static org.edx.jenkins.dsl.JenkinsPublicConstants.JENKINS_PUBLIC_LOG_ROTATOR
import static org.edx.jenkins.dsl.JenkinsPublicConstants.GHPRB_CANCEL_BUILDS_ON_UPDATE

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

Map publicPythonJobConfig = [
    open: true,
    jobName: 'edx-platform-python-pipeline-pr-wtw',
    repoName: 'edx-platform',
    whitelistBranchRegex: /^((?!open-release\/).)*$/,
    context: 'jenkins/python-wtw',
    onlyTriggerPhrase: true,
    triggerPhrase: /.*jenkins\W+run\W+wtw\W+python.*/,
    jenkinsFileDir: 'scripts/Jenkinsfiles',
    jenkinsFileName: 'python',
    pythonVersion: '3.8',
]

List jobConfigs = [
    publicPythonJobConfig
]

/* Iterate over the job configurations */
jobConfigs.each { jobConfig ->

    // This is the job DSL responsible for creating the main pipeline job.
    pipelineJob(jobConfig.jobName) {

        definition {

            if (!jobConfig.open.toBoolean()) {
                authorization GENERAL_PRIVATE_JOB_SECURITY()
            }
            properties {
                githubProjectUrl("https://github.com/edx/${jobConfig.repoName}/")
            }
            logRotator JENKINS_PUBLIC_LOG_ROTATOR(7)
            environmentVariables(
                REPO_NAME: "${jobConfig.repoName}",
                TOX_ENV: "${jobConfig.toxEnv}",
                FILTER_WHO_TESTS_WHAT: "true",
                PYTHON_VERSION: "${jobConfig.pythonVersion}"
            )

            triggers {
                githubPullRequest {
                    admins(ghprbMap['admin'])
                    useGitHubHooks()
                    triggerPhrase(jobConfig.triggerPhrase)
                    onlyTriggerPhrase(jobConfig.onlyTriggerPhrase)
                    userWhitelist(ghprbMap['userWhiteList'])
                    orgWhitelist(ghprbMap['orgWhiteList'])
                    whiteListTargetBranches([jobConfig.whitelistBranchRegex])
                    extensions {
                        commitStatus {
                            context(jobConfig.context)
                        }
                    }
                }
            }

            configure GHPRB_CANCEL_BUILDS_ON_UPDATE(false)

            cpsScm {
                scm {
                    git {
                        extensions {
                            cleanBeforeCheckout()
                            cloneOptions {
                                honorRefspec(true)
                                noTags(true)
                                shallow(true)
                            }
                            // To speed up builds, do a sparse checkout of just
                            // the files needed to run the pipeline. However, in
                            // case old branches/forks trigger this job, check
                            // out the 'scripts' directory to avoid the case
                            // where Jenkins tries to do a sparse check out of
                            // non-existent files (and corrupts the git state)
                            sparseCheckoutPaths {
                                sparseCheckoutPaths {
                                    sparseCheckoutPath {
                                        path('scripts')
                                    }
                                }
                            }
                        }
                        remote {
                            credentials('jenkins-worker')
                            github("edx/${jobConfig.repoName}", 'ssh', 'github.com')
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

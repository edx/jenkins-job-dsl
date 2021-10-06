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
    jobName: 'edx-platform-python-pipeline-pr',
    repoName: 'edx-platform',
    whitelistBranchRegex: /^((?!open-release\/).)*$/,
    context: 'jenkins/python',
    onlyTriggerPhrase: false,
    triggerPhrase: /.*jenkins\W+run\W+python.*/,
    jenkinsFileDir: 'scripts/Jenkinsfiles',
    jenkinsFileName: 'python',
    pythonVersion: '3.8',
]

Map django30PythonJobConfig = [
    open: true,
    jobName: 'edx-platform-django-3.0-python-pipeline-pr',
    repoName: 'edx-platform',
    whitelistBranchRegex: /^((?!open-release\/).)*$/,
    context: 'jenkins/django-3.0/python',
    onlyTriggerPhrase: true,
    triggerPhrase: /.*jenkins\W+run\W+django30\W+python.*/,
    jenkinsFileDir: 'scripts/Jenkinsfiles',
    jenkinsFileName: 'python',
    pythonVersion: '3.8',
    toxEnv: 'py38-django30',
]

Map django31PythonJobConfig = [
    open: true,
    jobName: 'edx-platform-django-3.1-python-pipeline-pr',
    repoName: 'edx-platform',
    whitelistBranchRegex: /^((?!open-release\/).)*$/,
    context: 'jenkins/django-3.1/python',
    onlyTriggerPhrase: true,
    triggerPhrase: /.*jenkins\W+run\W+django31\W+python.*/,
    jenkinsFileDir: 'scripts/Jenkinsfiles',
    jenkinsFileName: 'python',
    pythonVersion: '3.8',
    toxEnv: 'py38-django31',
]

Map django32PythonJobConfig = [
    open: true,
    jobName: 'edx-platform-django-3.2-python-pipeline-pr',
    repoName: 'edx-platform',
    whitelistBranchRegex: /^((?!open-release\/).)*$/,
    context: 'jenkins/django-3.2/python',
    onlyTriggerPhrase: false,
    triggerPhrase: /.*jenkins\W+run\W+django32\W+python.*/,
    jenkinsFileDir: 'scripts/Jenkinsfiles',
    jenkinsFileName: 'python',
    pythonVersion: '3.8',
    toxEnv: 'py38-django32',
]

Map privatePythonJobConfig = [
    open: false,
    jobName: 'edx-platform-python-pipeline-pr_private',
    repoName: 'edx-platform-private',
    whitelistBranchRegex: /^((?!open-release\/).)*$/,
    context: 'jenkins/python',
    onlyTriggerPhrase: false,
    triggerPhrase: /.*jenkins\W+run\W+python.*/,
    jenkinsFileDir: 'scripts/Jenkinsfiles',
    jenkinsFileName: 'python',
    pythonVersion: '3.8',
]

Map publicQualityJobConfig = [
    open: true,
    jobName: 'edx-platform-quality-pipeline-pr',
    repoName: 'edx-platform',
    whitelistBranchRegex: /^((?!open-release\/).)*$/,
    context: 'jenkins/quality',
    onlyTriggerPhrase: false,
    triggerPhrase: /.*jenkins\W+run\W+quality.*/,
    jenkinsFileDir: 'scripts/Jenkinsfiles',
    jenkinsFileName: 'quality',
    pythonVersion: '3.8',
]

Map django32QualityJobConfig = [
    open: true,
    jobName: 'edx-platform-django-3.2-quality-pipeline-pr',
    repoName: 'edx-platform',
    whitelistBranchRegex: /^((?!open-release\/).)*$/,
    context: 'jenkins/django-3.2/quality',
    onlyTriggerPhrase: false,
    triggerPhrase: /.*jenkins\W+run\W+django32\W+quality.*/,
    jenkinsFileDir: 'scripts/Jenkinsfiles',
    jenkinsFileName: 'quality',
    pythonVersion: '3.8',
    toxEnv: 'py38-django32',
]

Map privateQualityJobConfig = [
    open: false,
    jobName: 'edx-platform-quality-pipeline-pr_private',
    repoName: 'edx-platform-private',
    whitelistBranchRegex: /^((?!open-release\/).)*$/,
    context: 'jenkins/quality',
    onlyTriggerPhrase: false,
    triggerPhrase: /.*jenkins\W+run\W+quality.*/,
    jenkinsFileDir: 'scripts/Jenkinsfiles',
    jenkinsFileName: 'quality',
    pythonVersion: '3.8',
]

List jobConfigs = [
    publicPythonJobConfig,
    django30PythonJobConfig,
    django31PythonJobConfig,
    django32PythonJobConfig,
    privatePythonJobConfig,
    publicQualityJobConfig,
    django32QualityJobConfig,
    privateQualityJobConfig,
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
                PYTHON_VERSION: "${jobConfig.pythonVersion}"
            )

            triggers {
                githubPullRequest {
                    admins(ghprbMap['admin'])
                    useGitHubHooks()
                    triggerPhrase(jobConfig.triggerPhrase)
                    if (jobConfig.onlyTriggerPhrase) {
                        onlyTriggerPhrase(true)
                    }
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
                            pruneBranches()
                            pruneStaleBranch()
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

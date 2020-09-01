package platform

import org.yaml.snakeyaml.Yaml
import static org.edx.jenkins.dsl.JenkinsPublicConstants.JENKINS_PUBLIC_LOG_ROTATOR
import static org.edx.jenkins.dsl.JenkinsPublicConstants.JENKINS_PUBLIC_JUNIT_REPORTS
import static org.edx.jenkins.dsl.JenkinsPublicConstants.GHPRB_CANCEL_BUILDS_ON_UPDATE
import static org.edx.jenkins.dsl.JenkinsPublicConstants.GENERAL_PRIVATE_JOB_SECURITY
import static org.edx.jenkins.dsl.JenkinsPublicConstants.JENKINS_EDX_PLATFORM_TEST_NOTIFIER

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

// This script generates a lot of jobs. Here is the breakdown of the configuration options:
// Map exampleConfig = [
//     open: true/false if this job should be 'open' (use the default security scheme or not)
//     jobName: name of the job
//     repoName: name of the github repo containing the edx-platform you want to test
//     workerLabel: label of the worker to run this job on
//     whiteListBranchRegex: regular expression to filter which branches of a particular repo
//     can will trigger builds (via GHRPB)
//     context: Github context used to report test status
//     triggerPhrase: Github comment used to trigger this job
// ]

Map publicJobConfig = [
    open : true,
    jobName : 'edx-platform-accessibility-pr',
    repoName : 'edx-platform',
    workerLabel: 'js-worker',
    whitelistBranchRegex: /^((?!open-release\/).)*$/,
    context: 'jenkins/a11y',
    triggerPhrase: /.*jenkins\W+run\W+a11y.*/,
    pythonVersion: '3.5',
]

Map python38JobConfig = [
    open: true,
    jobName: 'edx-platform-python-3.8-accessibility-pr',
    repoName: 'edx-platform',
    workerLabel: 'js-worker',
    whitelistBranchRegex: /^((?!open-release\/).)*$/,
    context: 'jenkins/python-3.8/a11y',
    triggerPhrase: /.*jenkins\W+run\W+py38\W+a11y.*/,
    pythonVersion: '3.8',
]

Map django30JobConfig = [
    open: true,
    jobName: 'edx-platform-django-3.0-accessibility-pr',
    repoName: 'edx-platform',
    workerLabel: 'js-worker',
    whitelistBranchRegex: /^((?!open-release\/).)*$/,
    context: 'jenkins/django-3.0/a11y',
    onlyTriggerPhrase: true,
    triggerPhrase: /.*jenkins\W+run\W+django30\W+a11y.*/,
    pythonVersion: '3.8',
    toxEnv: 'py38-django30',
]

Map privateJobConfig = [
    open: false,
    jobName: 'edx-platform-accessibility-pr_private',
    repoName: 'edx-platform-private',
    workerLabel: 'js-worker',
    whitelistBranchRegex: /^((?!open-release\/).)*$/,
    context: 'jenkins/a11y',
    triggerPhrase: /.*jenkins\W+run\W+a11y.*/,
    pythonVersion: '3.5',
]

Map publicIronwoodJobConfig = [
    open: true,
    jobName: 'ironwood-accessibility-pr',
    repoName: 'edx-platform',
    workerLabel: 'ironwood-jenkins-worker',
    whitelistBranchRegex: /open-release\/ironwood.master/,
    context: 'jenkins/ironwood/a11y',
    triggerPhrase: /.*ironwood\W+run\W+a11y.*/
]

Map privateIronwoodJobConfig = [
    open: false,
    jobName: 'ironwood-accessibility-pr_private',
    repoName: 'edx-platform-private',
    workerLabel: 'ironwood-jenkins-worker',
    whitelistBranchRegex: /open-release\/ironwood.master/,
    context: 'jenkins/ironwood/a11y',
    triggerPhrase: /.*ironwood\W+run\W+a11y.*/
]

Map publicJuniperJobConfig = [
    open: true,
    jobName: 'juniper-accessibility-pr',
    repoName: 'edx-platform',
    workerLabel: 'juniper-jenkins-worker',
    whitelistBranchRegex: /open-release\/juniper.master/,
    context: 'jenkins/juniper/a11y',
    triggerPhrase: /.*juniper\W+run\W+a11y.*/
]

List jobConfigs = [
    publicJobConfig,
    python38JobConfig,
    django30JobConfig,
    privateJobConfig,
    publicIronwoodJobConfig,
    privateIronwoodJobConfig,
    publicJuniperJobConfig,
]

/* Iterate over the job configurations */
jobConfigs.each { jobConfig ->

    job(jobConfig.jobName) {

        if (!jobConfig.open.toBoolean()) {
            authorization GENERAL_PRIVATE_JOB_SECURITY()
        }
        properties {
              githubProjectUrl("https://github.com/edx/${jobConfig.repoName}/")
        }
        logRotator JENKINS_PUBLIC_LOG_ROTATOR(7)
        concurrentBuild()
        environmentVariables {
            env('PYTHON_VERSION', jobConfig.pythonVersion)
            env('TOX_ENV', jobConfig.toxEnv)
        }
        parameters {
            labelParam('WORKER_LABEL') {
                description('Select a Jenkins worker label for running this job')
                defaultValue(jobConfig.workerLabel)
            }
        }
        scm {
           git {
                remote {
                    url("git@github.com:edx/${jobConfig.repoName}.git")
                    refspec('+refs/pull/*:refs/remotes/origin/pr/*')
                    credentials('jenkins-worker')
                }
                branch('\${ghprbActualCommit}')
                browser()
                extensions {
                    relativeTargetDirectory(jobConfig.repoName)
                    cloneOptions {
                        // Use a reference clone for quicker clones. This is configured on jenkins workers via
                        // (https://github.com/edx/configuration/blob/master/playbooks/roles/test_build_server/tasks/main.yml#L26)
                        reference("\$HOME/edx-platform-clone")
                        timeout(10)
                    }
                    cleanBeforeCheckout()
                }
            }
        }

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

        wrappers {
           timeout {
               absolute(65)
           }
           timestamps()
           colorizeOutput('gnome-terminal')
           sshAgent('jenkins-worker')
           credentialsBinding {
               string('AWS_ACCESS_KEY_ID', 'DB_CACHE_ACCESS_KEY_ID')
               string('AWS_SECRET_ACCESS_KEY', 'DB_CACHE_SECRET_ACCESS_KEY')
           }
       }
       steps {
           shell("cd ${jobConfig.repoName}; TEST_SUITE=a11y bash scripts/accessibility-tests.sh")
       }
       publishers {
           archiveArtifacts {
               pattern(JENKINS_PUBLIC_JUNIT_REPORTS)
               pattern('edx-platform*/test_root/log/**/*.png')
               pattern('edx-platform*/test_root/log/**/*.log')
               allowEmpty()
               defaultExcludes()
           }
           archiveJunit(JENKINS_PUBLIC_JUNIT_REPORTS)
           if (jobConfig.repoName == "edx-platform") {
               downstreamParameterized JENKINS_EDX_PLATFORM_TEST_NOTIFIER.call("${jobConfig.repoName}", '${ghprbPullId}')
           }
       }
    }
}

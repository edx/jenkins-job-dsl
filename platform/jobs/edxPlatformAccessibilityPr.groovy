package platform

import org.yaml.snakeyaml.Yaml
import static org.edx.jenkins.dsl.JenkinsPublicConstants.JENKINS_PUBLIC_LOG_ROTATOR
import static org.edx.jenkins.dsl.JenkinsPublicConstants.JENKINS_PUBLIC_JUNIT_REPORTS
import static org.edx.jenkins.dsl.JenkinsPublicConstants.GHPRB_WHITELIST_BRANCH
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
    workerLabel: 'jenkins-worker',
    whitelistBranchRegex: /^((?!open-release\/).)*$/,
    context: 'jenkins/a11y',
    triggerPhrase: /.*jenkins\W+run\W+a11y.*/
]

Map privateJobConfig = [
    open: false,
    jobName: 'edx-platform-accessibility-pr_private',
    repoName: 'edx-platform-private',
    workerLabel: 'jenkins-worker',
    whitelistBranchRegex: /^((?!open-release\/).)*$/,
    context: 'jenkins/a11y',
    triggerPhrase: /.*jenkins\W+run\W+a11y.*/
]

Map publicHawthornJobConfig = [
    open: true,
    jobName: 'hawthorn-accessibility-pr',
    repoName: 'edx-platform',
    workerLabel: 'hawthorn-jenkins-worker',
    whitelistBranchRegex: /open-release\/hawthorn.beta1/,
    context: 'jenkins/hawthorn/a11y',
    triggerPhrase: /.*hawthorn\W+run\W+a11y.*/
]

Map privateHawthornJobConfig = [
    open: false,
    jobName: 'hawthorn-accessibility-pr_private',
    repoName: 'edx-platform-private',
    workerLabel: 'hawthorn-jenkins-worker',
    whitelistBranchRegex: /open-release\/hawthorn.beta1/,
    context: 'jenkins/hawthorn/a11y',
    triggerPhrase: /.*hawthorn\W+run\W+a11y.*/
]

Map publicGinkgoJobConfig = [
    open: true,
    jobName: 'ginkgo-accessibility-pr',
    repoName: 'edx-platform',
    workerLabel: 'ginkgo-jenkins-worker',
    whitelistBranchRegex: /open-release\/ginkgo.master/,
    context: 'jenkins/ginkgo/a11y',
    triggerPhrase: /.*ginkgo\W+run\W+a11y.*/
]

Map privateGinkgoJobConfig = [
    open: false,
    jobName: 'ginkgo-accessibility-pr_private',
    repoName: 'edx-platform-private',
    workerLabel: 'ginkgo-jenkins-worker',
    whitelistBranchRegex: /open-release\/ginkgo.master/,
    context: 'jenkins/ginkgo/a11y',
    triggerPhrase: /.*ginkgo\W+run\W+a11y.*/
]

Map publicFicusJobConfig = [
    open: true,
    jobName: 'ficus-accessibility-pr',
    repoName: 'edx-platform',
    workerLabel: 'ficus-jenkins-worker',
    whitelistBranchRegex: /open-release\/ficus.master/,
    context: 'jenkins/ficus/a11y',
    triggerPhrase: /.*ficus\W+run\W+a11y.*/
]

Map privateFicusJobConfig = [
    open: false,
    jobName: 'ficus-accessibility-pr_private',
    repoName: 'edx-platform-private',
    workerLabel: 'ficus-jenkins-worker',
    whitelistBranchRegex: /open-release\/ficus.master/,
    context: 'jenkins/ficus/a11y',
    triggerPhrase: /.*ficus\W+run\W+a11y.*/
]

Map python3JobConfig = [
    open : true,
    jobName : 'edx-platform-python3-accessibility-pr',
    repoName : 'edx-platform',
    workerLabel: 'jenkins-worker',
    whitelistBranchRegex: /^((?!open-release\/).)*$/,
    context: 'jenkins/python3.5/a11y',
    triggerPhrase: /.*jenkins\W+run\W+py35-django111\W+a11y.*/,
    commentOnly: true,
    toxEnv: 'py35-django111'
]

List jobConfigs = [
    publicJobConfig,
    privateJobConfig,
    publicHawthornJobConfig,
    privateHawthornJobConfig,
    publicGinkgoJobConfig,
    privateGinkgoJobConfig,
    publicFicusJobConfig,
    privateFicusJobConfig,
    python3JobConfig
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
                if (jobConfig.commentOnly) {
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
           publishHtml {
               report("${jobConfig.repoName}/reports/pa11ycrawler/html") {
               reportName('HTML Report')
               allowMissing()
               keepAll()
               }
           }
           archiveArtifacts {
               pattern(JENKINS_PUBLIC_JUNIT_REPORTS)
               pattern('edx-platform*/test_root/log/**/*.png')
               pattern('edx-platform*/test_root/log/**/*.log')
               pattern('edx-platform*/reports/pa11ycrawler/**/*')
               allowEmpty()
               defaultExcludes()
           }
           archiveJunit(JENKINS_PUBLIC_JUNIT_REPORTS)
           if (jobConfig.repoName == "edx-platform") {
               downstreamParameterized JENKINS_EDX_PLATFORM_TEST_NOTIFIER.call('${ghprbPullId}')
           }
       }
    }
}

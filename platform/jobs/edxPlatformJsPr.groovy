package platform

import org.yaml.snakeyaml.Yaml
import static org.edx.jenkins.dsl.JenkinsPublicConstants.JENKINS_PUBLIC_LOG_ROTATOR
import static org.edx.jenkins.dsl.JenkinsPublicConstants.JENKINS_PUBLIC_JUNIT_REPORTS
import static org.edx.jenkins.dsl.JenkinsPublicConstants.GHPRB_CANCEL_BUILDS_ON_UPDATE
import static org.edx.jenkins.dsl.JenkinsPublicConstants.GENERAL_PRIVATE_JOB_SECURITY
import static org.edx.jenkins.dsl.JenkinsPublicConstants.JENKINS_EDX_PLATFORM_TEST_NOTIFIER
import static org.edx.jenkins.dsl.JenkinsPublicConstants.JENKINS_PUBLIC_GITHUB_STATUS_UNSTABLE_OR_WORSE

String archiveReports = 'edx-platform*/reports/**/*,edx-platform*/test_root/log/*.png,'
archiveReports += 'edx-platform*/test_root/log/*.log,'
archiveReports += 'edx-platform*/**/nosetests.xml,edx-platform*/**/TEST-*.xml'

String descriptionString = 'This job runs pull requests through our javascript tests.<br><br> \n'
descriptionString += 'To run failed tests on devstack, see test patterns <a href=https://github.'
descriptionString += 'com/edx/edx-platform/blob/master/docs/en_us/internal/testing.rst>here</a>'

/* stdout logger */
Map config = [:]
Binding bindings = getBinding()
config.putAll(bindings.getVariables())
PrintStream out = config['out']

/* Map to hold the k:v pairs parsed from the secret file */
Map ghprbMap = [
    admin: ['alexei.kornienko@raccoongang.com'],
    userWhiteList: ['alexei.kornienko@raccoongang.com'],
    orgWhiteList: ['raccoongang'],
]

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
    jobName : 'edx-platform-js-pr',
    repoName: 'edx-platform',
    workerLabel: 'jenkins-worker',
    whitelistBranchRegex: /^((?!open-release\/).)*$/,
    context: 'jenkins/js',
    triggerPhrase: /.*jenkins\W+run\W+js.*/
]

Map publicHawthornJobConfig = [
    open: true,
    jobName: 'hawthorn-js-pr',
    repoName: 'edx-platform',
    workerLabel: 'hawthorn-jenkins-worker',
    whitelistBranchRegex: /open-release\/hawthorn.master/,
    context: 'jenkins/hawthorn/js',
    triggerPhrase: /.*hawthorn\W+run\W+js.*/
]

Map publicGinkgoJobConfig = [
    open: true,
    jobName: 'ginkgo-js-pr',
    repoName: 'edx-platform',
    workerLabel: 'ginkgo-jenkins-worker',
    whitelistBranchRegex: /open-release\/ginkgo.master/,
    context: 'jenkins/ginkgo/js',
    triggerPhrase: /.*ginkgo\W+run\W+js.*/
]

Map publicFicusJobConfig = [
    open: true,
    jobName: 'ficus-js-pr',
    repoName: 'edx-platform',
    workerLabel: 'ficus-jenkins-worker',
    whitelistBranchRegex: /open-release\/ficus.master/,
    context: 'jenkins/ficus/js',
    triggerPhrase: /.*ficus\W+run\W+js.*/
]

Map python3JobConfig = [
    open : true,
    jobName : 'edx-platform-python3-js-pr',
    repoName: 'edx-platform',
    workerLabel: 'jenkins-worker',
    whitelistBranchRegex: /^((?!open-release\/).)*$/,
    context: 'jenkins/python3.5/js',
    triggerPhrase: /.*jenkins\W+run\W+py35-django111\W+js.*/,
    commentOnly: true,
    toxEnv: 'py35-django111'
]

List jobConfigs = [
    publicJobConfig,
    publicHawthornJobConfig,
    publicGinkgoJobConfig,
    publicFicusJobConfig,
    python3JobConfig
]

/* Iterate over the job configurations */
jobConfigs.each { jobConfig ->

    job(jobConfig['jobName']) {

        description(descriptionString)
        if (!jobConfig.open.toBoolean()) {
            authorization GENERAL_PRIVATE_JOB_SECURITY()
        }
        properties {
              githubProjectUrl("https://github.com/raccoongang/${jobConfig.repoName}/")
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
                userWhitelist(ghprbMap['userWhiteList'])
                orgWhitelist(ghprbMap['orgWhiteList'])
                triggerPhrase(jobConfig.triggerPhrase)
                if (jobConfig.commentOnly) {
                    onlyTriggerPhrase(true)
                }
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
                absolute(45)
            }
            timestamps()
            colorizeOutput()
            buildName('#${BUILD_NUMBER}: Javascript Tests')
            sshAgent('jenkins-worker')
        }
        steps {
            shell("cd ${jobConfig.repoName}; TEST_SUITE=js-unit ./scripts/all-tests.sh")
        }
        publishers {
            archiveArtifacts {
                pattern(archiveReports)
                defaultExcludes()
            }
            cobertura ('edx-platform*/**/reports/**/coverage*.xml') {
                failNoReports(true)
                sourceEncoding('ASCII')
                methodTarget(80, 0, 0)
                lineTarget(80, 0, 0)
                conditionalTarget(70, 0, 0)
            }
            publishHtml {
                report('edx-platform*/reports') {
                    reportFiles('diff_coverage_combined.html')
                    reportName('Diff Coverage Report')
                    keepAll()
                    allowMissing()
                }
            }
            archiveJunit(JENKINS_PUBLIC_JUNIT_REPORTS)
            if (jobConfig.repoName == "edx-platform") {
                downstreamParameterized JENKINS_EDX_PLATFORM_TEST_NOTIFIER.call('${ghprbPullId}')
            }
        }
    }
}

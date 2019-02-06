package platform

import org.yaml.snakeyaml.Yaml
import static org.edx.jenkins.dsl.JenkinsPublicConstants.JENKINS_PUBLIC_ARCHIVE_XUNIT
import static org.edx.jenkins.dsl.JenkinsPublicConstants.JENKINS_PUBLIC_LOG_ROTATOR
import static org.edx.jenkins.dsl.JenkinsPublicConstants.JENKINS_PUBLIC_GITHUB_BASEURL
import static org.edx.jenkins.dsl.JenkinsPublicConstants.GHPRB_CANCEL_BUILDS_ON_UPDATE
import static org.edx.jenkins.dsl.JenkinsPublicConstants.GENERAL_PRIVATE_JOB_SECURITY
import static org.edx.jenkins.dsl.JenkinsPublicConstants.JENKINS_EDX_PLATFORM_TEST_NOTIFIER

/* stdout logger */
/* use this instead of println, because you can pass it into closures or other scripts. */
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

String archiveReports = 'reports/**/*,test_root/log/*.log,'
archiveReports += 'edx-platform*/reports/**/*,edx-platform*/test_root/log/*.log,'

String pep8Reports = 'reports/pep8/pep8.report, edx-platform*/reports/pep8/pep8.report'
String pylintReports = 'reports/**/pylint.report, edx-platform*/reports/**/pylint.report'

// This script generates a lot of jobs. Here is the breakdown of the configuration options:
// Map exampleConfig = [
//     open: true/false if this job should be 'open' (use the default security scheme or not)
//     jobName: name of the job
//     subsetJob: name of subset job run by this job (shard jobs)
//     repoName: name of the github repo containing the edx-platform you want to test
//     workerLabel: label of the worker to run this job on
//     whiteListBranchRegex: regular expression to filter which branches of a particular repo
//     can will trigger builds (via GHRPB)
//     context: Github context used to report test status
//     triggerPhrase: Github comment used to trigger this job
//     defaultTestengbranch: default branch of the testeng-ci repo for this job
//     diffJob: Job to run diff coverage
// ]

Map publicJobConfig = [
    open : true,
    jobName : 'edx-platform-quality-flow-pr',
    subsetJob: 'edx-platform-test-subset',
    repoName: 'edx-platform',
    workerLabel: 'jenkins-worker',
    whitelistBranchRegex: /^((?!open-release\/).)*$/,
    context: 'jenkins/quality',
    triggerPhrase: /.*jenkins\W+run\W+quality.*/,
    defaultTestengBranch: 'master',
    targetBranch: 'origin/master',
    diffJob: 'edx-platform-quality-diff'
]

Map hawthornJobConfig = [
    open: true,
    jobName: 'hawthorn-quality-flow-pr',
    subsetJob: 'edx-platform-test-subset',
    repoName: 'edx-platform',
    workerLabel: 'hawthorn-jenkins-worker',
    whitelistBranchRegex: /open-release\/hawthorn.master/,
    context: 'jenkins/hawthorn/quality',
    triggerPhrase: /.*hawthorn\W+run\W+quality.*/,
    defaultTestengBranch: 'origin/open-release/hawthorn.master',
    targetBranch: 'origin/open-release/hawthorn.master',
    diffJob: 'edx-platform-quality-diff'
]

Map python3JobConfig = [
    open : true,
    jobName : 'edx-platform-python3-quality-flow-pr',
    subsetJob: 'edx-platform-test-subset',
    repoName: 'edx-platform',
    workerLabel: 'jenkins-worker',
    whitelistBranchRegex: /^((?!open-release\/).)*$/,
    context: 'jenkins/python3.5/quality',
    triggerPhrase: /.*jenkins\W+run\W+py35-django111\W+quality.*/,
    defaultTestengBranch: 'master',
    targetBranch: 'origin/master',
    diffJob: 'edx-platform-quality-diff',
    commentOnly: true,
    toxEnv: 'py35-django111'
]

List jobConfigs = [
    publicJobConfig,
    hawthornJobConfig,
    python3JobConfig
]

jobConfigs.each { jobConfig ->

    buildFlowJob(jobConfig.jobName) {

        if (!jobConfig.open.toBoolean()) {
            authorization GENERAL_PRIVATE_JOB_SECURITY()
        }
        properties {
            githubProjectUrl("https://github.com/raccoongang/${jobConfig.repoName}/")
        }
        logRotator JENKINS_PUBLIC_LOG_ROTATOR(7)
        concurrentBuild()
        label('flow-worker-quality')
        checkoutRetryCount(5)
        environmentVariables {
            env('SUBSET_JOB', jobConfig.subsetJob)
            env('REPO_NAME', jobConfig.repoName)
            env('DIFF_JOB', jobConfig.diffJob)
            env('TARGET_BRANCH', jobConfig.targetBranch)
            env('TOX_ENV', jobConfig.toxEnv)
        }
        parameters {
            stringParam('WORKER_LABEL', jobConfig.workerLabel, 'Jenkins worker for running the test subset jobs')
        }
        multiscm {
            git {
                remote {
                    url('https://github.com/edx/testeng-ci.git')
                }
                branch(jobConfig.defaultTestengBranch)
                browser()
                extensions {
                    cleanBeforeCheckout()
                    relativeTargetDirectory('testeng-ci')
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
                absolute(90)
            }
            timestamps()
            preBuildCleanup {
                includePattern(archiveReports)
                deleteDirectories()
            }
            colorizeOutput()
        }

        dslFile('testeng-ci/jenkins/flow/pr/edx-platform-quality-pr.groovy')

        publishers {
            archiveArtifacts {
                pattern(archiveReports)
                defaultExcludes(true)
                allowEmpty(true)
            }
            archiveXUnit JENKINS_PUBLIC_ARCHIVE_XUNIT()
            publishHtml {
                report("reports/metrics/") {
                    reportName('Quality Report')
                    reportFiles(
                        'pylint/*view*/,pep8/*view*/,python_complexity/*view*/,' +
                        'xsscommitlint/*view*/,xsslint/*view*/,eslint/*view*/'
                    )
                    keepAll(true)
                    allowMissing(true)
                }
                report("reports/diff_quality") {
                    reportName('Diff Quality Report')
                    reportFiles('diff_quality_pylint.html, diff_quality_eslint.html')
                    keepAll(true)
                    allowMissing(true)
                }
            }
            warnings([], ['Pep8': pep8Reports, 'PYLint': pylintReports]) {
                canRunOnFailed()
            }
            if (jobConfig.repoName == "edx-platform") {
                downstreamParameterized JENKINS_EDX_PLATFORM_TEST_NOTIFIER('${ghprbPullId}')
            }
        }
    }
}

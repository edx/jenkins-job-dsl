package platform

import org.yaml.snakeyaml.Yaml
import static org.edx.jenkins.dsl.JenkinsPublicConstants.JENKINS_PUBLIC_LOG_ROTATOR
import static org.edx.jenkins.dsl.JenkinsPublicConstants.JENKINS_PUBLIC_GITHUB_BASEURL
import static org.edx.jenkins.dsl.JenkinsPublicConstants.GHPRB_WHITELIST_BRANCH
import static org.edx.jenkins.dsl.JenkinsPublicConstants.GENERAL_PRIVATE_JOB_SECURITY

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
    triggerPhrase: 'jenkins run quality',
    defaultTestengBranch: 'master',
    diffJob: 'edx-platform-quality-diff'
]

Map privateJobConfig = [
    open: false,
    jobName: 'edx-platform-quality-flow-pr_private',
    subsetJob: 'edx-platform-test-subset',
    repoName: 'edx-platform-private',
    workerLabel: 'jenkins-worker',
    whitelistBranchRegex: /^((?!open-release\/).)*$/,
    context: 'jenkins/quality',
    triggerPhrase: 'jenkins run quality',
    defaultTestengBranch: 'master',
    diffJob: 'edx-platform-quality-diff_private'
]

List jobConfigs = [
    publicJobConfig,
    privateJobConfig,
]

jobConfigs.each { jobConfig ->

    buildFlowJob(jobConfig.jobName) {

        if (!jobConfig.open.toBoolean()) {
            authorization GENERAL_PRIVATE_JOB_SECURITY()
        }
        properties {
            githubProjectUrl("https://github.com/edx/${jobConfig.repoName}/")
        }
        logRotator JENKINS_PUBLIC_LOG_ROTATOR(7)
        concurrentBuild()
        label('flow-worker-quality')
        checkoutRetryCount(5)
        environmentVariables {
            env('SUBSET_JOB', jobConfig.subsetJob)
            env('REPO_NAME', jobConfig.repoName)
            env('DIFF_JOB', jobConfig.diffJob)
            env('TARGET_BRANCH', 'origin/master')
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
           git {
                remote {
                    url("https://github.com/edx/${jobConfig.repoName}.git")
                    refspec('+refs/pull/*:refs/remotes/origin/pr/*')
                    if (!jobConfig['open'].toBoolean()) {
                        credentials("EDX_STATUS_BOT_CREDENTIALS")
                    }
                }
                branch('\${ghprbActualCommit}')
                browser()
                extensions {
                    relativeTargetDirectory(jobConfig.repoName)
                    cloneOptions {
                        reference("\$HOME/edx-platform-clone")
                        timeout(10)
                    }
                    cleanBeforeCheckout()
                }
            }
        }
        triggers {
            pullRequest {
                admins(ghprbMap['admin'])
                useGitHubHooks()
                triggerPhrase(jobConfig.triggerPhrase)
                userWhitelist(ghprbMap['userWhiteList'])
                orgWhitelist(ghprbMap['orgWhiteList'])
                extensions {
                    commitStatus {
                        context(jobConfig.context)
                    }
                }
            }
        }
        wrappers {
            timeout {
                absolute(90)
            }
            timestamps()
            colorizeOutput()
        }

        configure GHPRB_WHITELIST_BRANCH(jobConfig.whitelistBranchRegex)

        dslFile('testeng-ci/jenkins/flow/pr/edx-platform-quality-pr.groovy')

        publishers {
            archiveArtifacts {
                pattern(
                    'reports/**/*,test_root/log/*.log,' +
                    'edx-platform*/reports/**/*,edx-platform*/test_root/log/*.log,'
                )
                defaultExcludes(true)
                allowEmpty(true)
            }
            publishHtml {
                report("${jobConfig.repoName}/reports/metrics/") {
                    reportName('Quality Report')
                    reportFiles(
                        'pylint/*view*/,pep8/*view*/,python_complexity/*view*/,' +
                        'xsscommitlint/*view*/,xsslint/*view*/,eslint/*view*/'
                    )
                    keepAll(true)
                    allowMissing(true)
                }
                report("${jobConfig.repoName}/reports/diff_quality") {
                    reportName('Diff Quality Report')
                    reportFiles('diff_quality_pylint.html, diff_quality_eslint.html')
                    keepAll(true)
                    allowMissing(true)
                }
            }
        }
    }
}

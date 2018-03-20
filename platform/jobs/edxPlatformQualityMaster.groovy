package devops

import org.yaml.snakeyaml.Yaml
import static org.edx.jenkins.dsl.JenkinsPublicConstants.GENERAL_PRIVATE_JOB_SECURITY
import static org.edx.jenkins.dsl.JenkinsPublicConstants.JENKINS_PUBLIC_LOG_ROTATOR
import static org.edx.jenkins.dsl.JenkinsPublicConstants.JENKINS_PUBLIC_HIPCHAT
import static org.edx.jenkins.dsl.JenkinsPublicConstants.JENKINS_PUBLIC_BASE_URL
import static org.edx.jenkins.dsl.JenkinsPublicConstants.JENKINS_PUBLIC_GITHUB_STATUS_PENDING
import static org.edx.jenkins.dsl.JenkinsPublicConstants.JENKINS_PUBLIC_GITHUB_STATUS_SUCCESS
import static org.edx.jenkins.dsl.JenkinsPublicConstants.JENKINS_PUBLIC_GITHUB_STATUS_UNSTABLE_OR_WORSE
import static org.edx.jenkins.dsl.JenkinsPublicConstants.JENKINS_PUBLIC_GITHUB_BASEURL

String archiveReports = 'reports/**/*,test_root/log/*.log,'
archiveReports += 'edx-platform*/reports/**/*,edx-platform*/test_root/log/*.log,'

String htmlReports = 'pylint/*view*/,pep8/*view*/,python_complexity/*view*/,'
htmlReports += 'xsscommitlint/*view*/,xsslint/*view*/,eslint/*view*/'

/* stdout logger */
/* use this instead of println, because you can pass it into closures or other scripts. */
/* TODO: Move this into JenkinsPublicConstants, as it can be shared. */
Map config = [:]
Binding bindings = getBinding()
config.putAll(bindings.getVariables())
PrintStream out = config['out']

// This script generates a lot of jobs. Here is the breakdown of the configuration options:
// Map exampleConfig = [
//     open: true/false if this job should be 'open' (use the default security scheme or not)
//     jobName: name of the job
//     subsetJob: name of subset job run by this job (shard jobs)
//     repoName: name of the github repo containing the edx-platform you want to test
//     workerLabel: label of the worker to run the subset jobs on
//     context: Github context used to report test status
//     defaultTestengbranch: default branch of the testeng-ci repo for this job
//     refSpec: refspec for branches to build
//     defaultBranch: branch to build
// ]

Map publicJobConfig = [
    open: true,
    jobName: 'edx-platform-quality-flow-master',
    subsetJob: 'edx-platform-test-subset',
    repoName: 'edx-platform',
    workerLabel: 'jenkins-worker',
    context: 'jenkins/quality',
    defaultTestengBranch: 'master',
    refSpec : '+refs/heads/master:refs/remotes/origin/master',
    defaultBranch : 'master'
]

Map privateJobConfig = [
    open: false,
    jobName: 'edx-platform-quality-flow-master_private',
    subsetJob: 'edx-platform-test-subset',
    repoName: 'edx-platform-private',
    workerLabel: 'jenkins-worker',
    context: 'jenkins/quality',
    defaultTestengBranch: 'master',
    refSpec : '+refs/heads/master:refs/remotes/origin/master',
    defaultBranch : 'master'
]

List jobConfigs = [
    publicJobConfig,
    privateJobConfig
]

/* Iterate over the job configurations */
jobConfigs.each { jobConfig ->

    buildFlowJob(jobConfig.jobName) {

        /* For non-open jobs, enable project based security */
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
                    url("https://github.com/edx/${jobConfig.repoName}.git")
                    refspec(jobConfig.refSpec)
                    if (!jobConfig.open.toBoolean()) {
                        credentials("EDX_STATUS_BOT_CREDENTIALS")
                    }
                }
                branch(jobConfig.defaultBranch)
                browser()
                extensions {
                    cloneOptions {
                        reference("\$HOME/edx-platform-clone")
                        timeout(10)
                    }
                    cleanBeforeCheckout()
                    relativeTargetDirectory(jobConfig.repoName)
                }
            }
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
        triggers { githubPush() }
        wrappers {
            timeout {
               absolute(90)
            }
            timestamps()
            colorizeOutput()
       }

        Map <String, String> predefinedPropsMap  = [:]
        predefinedPropsMap.put('GIT_SHA', '${GIT_COMMIT}')
        predefinedPropsMap.put('GITHUB_ORG', 'edx')
        predefinedPropsMap.put('CONTEXT', jobConfig.context)
        steps { //trigger GitHub-Build-Status and run accessibility tests
            predefinedPropsMap.put('GITHUB_REPO', jobConfig.repoName)
            predefinedPropsMap.put('TARGET_URL', JENKINS_PUBLIC_BASE_URL + 'job/'
                                  + jobConfig.jobName + '/${BUILD_NUMBER}/')
            downstreamParameterized JENKINS_PUBLIC_GITHUB_STATUS_PENDING.call(predefinedPropsMap)
            shell("cd ${jobConfig.repoName}; TEST_SUITE=quality ./scripts/all-tests.sh")
        }
        dslFile('testeng-ci/jenkins/flow/master/edx-platform-quality-master.groovy')
        publishers { //publish artifacts, HTML, violations report, trigger GitHub-Build-Status, email, message hipchat
            archiveArtifacts {
                pattern(archiveReports)
                defaultExcludes()
            }
            publishHtml {
                report('reports/metrics/') {
                    reportFiles(htmlReports)
                    reportName('Quality Report')
                    allowMissing()
                    keepAll()
                }
            }
            violations(100) {
                checkstyle(10, 999, 999)
                codenarc(10, 999, 999)
                cpd(10, 999, 999)
                cpplint(10, 999, 999)
                csslint(10, 999, 999)
                findbugs(10, 999, 999)
                fxcop(10, 999, 999)
                gendarme(10, 999, 999)
                jcreport(10, 999, 999)
                jslint(10, 999, 999)
                pep8(1, 2, 3, '**/pep8.report')
                perlcritic(10, 999, 999)
                pmd(10, 999, 999)
                pylint(10, 10000, 10000, '**/*pylint.report')
                simian(10, 999, 999)
                stylecop(10, 999, 999)
                sourceEncoding()
            }
            downstreamParameterized JENKINS_PUBLIC_GITHUB_STATUS_SUCCESS.call(predefinedPropsMap)
            downstreamParameterized JENKINS_PUBLIC_GITHUB_STATUS_UNSTABLE_OR_WORSE.call(predefinedPropsMap)
            mailer('testeng@edx.org')
            hipChat JENKINS_PUBLIC_HIPCHAT.call('')  // Use the token specified in the global configuration
        }
    }
}

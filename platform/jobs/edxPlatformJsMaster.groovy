package platform

import org.yaml.snakeyaml.Yaml
import static org.edx.jenkins.dsl.JenkinsPublicConstants.GENERAL_PRIVATE_JOB_SECURITY
import static org.edx.jenkins.dsl.JenkinsPublicConstants.JENKINS_PUBLIC_LOG_ROTATOR
import static org.edx.jenkins.dsl.JenkinsPublicConstants.GENERAL_SLACK_STATUS
import static org.edx.jenkins.dsl.JenkinsPublicConstants.JENKINS_PUBLIC_BASE_URL
import static org.edx.jenkins.dsl.JenkinsPublicConstants.JENKINS_PUBLIC_JUNIT_REPORTS
import static org.edx.jenkins.dsl.JenkinsPublicConstants.JENKINS_PUBLIC_GITHUB_STATUS_PENDING
import static org.edx.jenkins.dsl.JenkinsPublicConstants.JENKINS_PUBLIC_GITHUB_STATUS_SUCCESS
import static org.edx.jenkins.dsl.JenkinsPublicConstants.JENKINS_PUBLIC_GITHUB_STATUS_UNSTABLE_OR_WORSE
import static org.edx.jenkins.dsl.JenkinsPublicConstants.JENKINS_PUBLIC_GITHUB_BASEURL

String archiveReports = 'edx-platform*/reports/**/*,edx-platform*/test_root/log/*.png,'
archiveReports += 'edx-platform*/test_root/log/*.log,'
archiveReports += 'edx-platform*/**/nosetests.xml,edx-platform*/**/TEST-*.xml'

/* stdout logger */
Map config = [:]
Binding bindings = getBinding()
config.putAll(bindings.getVariables())
PrintStream out = config['out']

// This script generates a lot of jobs. Here is the breakdown of the configuration options:
// Map exampleConfig = [
//     open: true/false if this job should be 'open' (use the default security scheme or not)
//     jobName: name of the job
//     repoName: name of the github repo containing the edx-platform you want to test
//     workerLabel: label of the worker to run the subset jobs on
//     context: Github context used to report test status
//     refSpec: refspec for branches to build
//     defaultBranch: branch to build
// ]

Map publicJobConfig = [
    open: true,
    jobName: 'edx-platform-js-master',
    repoName: 'edx-platform',
    workerLabel: 'jenkins-worker',
    context: 'jenkins/js',
    refSpec : '+refs/heads/master:refs/remotes/origin/master',
    defaultBranch : 'master'
]

Map hawthornJobConfig = [
    open: true,
    jobName: 'hawthorn-js-master',
    repoName: 'edx-platform',
    workerLabel: 'jenkins-worker',
    context: 'jenkins/hawthorn/js',
    refSpec : '+refs/heads/open-release/hawthorn.master:refs/remotes/origin/open-release/hawthorn.master',
    defaultBranch : 'refs/heads/open-release/hawthorn.master'
]

Map ginkgoJobConfig = [
    open: true,
    jobName: 'ginkgo-js-master',
    repoName: 'edx-platform',
    workerLabel: 'jenkins-worker',
    context: 'jenkins/ginkgo/js',
    refSpec : '+refs/heads/open-release/ginkgo.master:refs/remotes/origin/open-release/ginkgo.master',
    defaultBranch : 'refs/heads/open-release/ginkgo.master'
]

Map ficusJobConfig = [
    open: true,
    jobName: 'ficus-js-master',
    repoName: 'edx-platform',
    workerLabel: 'jenkins-worker',
    context: 'jenkins/ficus/js',
    refSpec : '+refs/heads/open-release/ficus.master:refs/remotes/origin/open-release/ficus.master',
    defaultBranch : 'refs/heads/open-release/ficus.master'
]

List jobConfigs = [
    publicJobConfig,
    hawthornJobConfig,
    ginkgoJobConfig,
    ficusJobConfig
]

/* Iterate over the job configurations */
jobConfigs.each { jobConfig ->

    job(jobConfig.jobName) {

        /* For non-open jobs, enable project based security */
        if (!jobConfig.open.toBoolean()) {
            authorization GENERAL_PRIVATE_JOB_SECURITY()
        }
        properties {
            githubProjectUrl("https://github.com/edx/${jobConfig.repoName}/")
        }
        logRotator JENKINS_PUBLIC_LOG_ROTATOR(7)
        concurrentBuild()
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
                    refspec(jobConfig.refSpec)
                    credentials('jenkins-worker')
                }
                branch(jobConfig.defaultBranch)
                browser()
                extensions {
                    cloneOptions {
                        // Use a reference clone for quicker clones. This is configured on jenkins workers via
                        // (https://github.com/edx/configuration/blob/master/playbooks/roles/test_build_server/tasks/main.yml#L26)
                        reference("\$HOME/edx-platform-clone")
                        timeout(10)
                    }
                    cleanBeforeCheckout()
                    relativeTargetDirectory(jobConfig.repoName)
                }
            }
        }
        triggers { githubPush() }
        wrappers {
            timeout {
                absolute(30)
            }
            timestamps()
            colorizeOutput()
            sshAgent('jenkins-worker')
            buildName('#${BUILD_NUMBER}: JS Tests')
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
               shell("cd ${jobConfig.repoName}; TEST_SUITE=js-unit ./scripts/all-tests.sh")
        }
        publishers { //archive artifacts, coverage, JUnit report, trigger GitHub-Build-Status, email, message slack
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
            archiveJunit(JENKINS_PUBLIC_JUNIT_REPORTS)
            downstreamParameterized JENKINS_PUBLIC_GITHUB_STATUS_SUCCESS.call(predefinedPropsMap)
            downstreamParameterized JENKINS_PUBLIC_GITHUB_STATUS_UNSTABLE_OR_WORSE.call(predefinedPropsMap)
            mailer('testeng@edx.org')
            configure GENERAL_SLACK_STATUS()
        }
    }
}

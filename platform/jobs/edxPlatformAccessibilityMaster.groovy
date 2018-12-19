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

/* stdout logger */
/* use this instead of println, because you can pass it into closures or other scripts. */
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
    jobName: 'edx-platform-accessibility-master',
    repoName: 'edx-platform',
    workerLabel: 'jenkins-worker',
    context: 'jenkins/a11y',
    refSpec : '+refs/heads/master:refs/remotes/origin/master',
    defaultBranch : 'master'
]

Map hawthornJobConfig = [
    open: true,
    jobName: 'hawthorn-accessibility-master',
    repoName: 'edx-platform',
    workerLabel: 'hawthorn-jenkins-worker',
    context: 'jenkins/hawthorn/a11y',
    refSpec : '+refs/heads/open-release/hawthorn.master:refs/remotes/origin/open-release/hawthorn.master',
    defaultBranch : 'refs/heads/open-release/hawthorn.master'
]

Map ginkgoJobConfig = [
    open: true,
    jobName: 'ginkgo-accessibility-master',
    repoName: 'edx-platform',
    workerLabel: 'ginkgo-jenkins-worker',
    context: 'jenkins/ginkgo/a11y',
    refSpec : '+refs/heads/open-release/ginkgo.master:refs/remotes/origin/open-release/ginkgo.master',
    defaultBranch : 'refs/heads/open-release/ginkgo.master'
]

Map ficusJobConfig = [
    open: true,
    jobName: 'ficus-accessibility-master',
    repoName: 'edx-platform',
    workerLabel: 'ficus-jenkins-worker',
    context: 'jenkins/ficus/a11y',
    refSpec : '+refs/heads/open-release/ficus.master:refs/remotes/origin/open-release/ficus.master',
    defaultBranch : 'refs/heads/open-release/ficus.master'
]

List jobConfigs = [
    publicJobConfig,
    hawthornJobConfig,
    ginkgoJobConfig,
    ficusJobConfig
]

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
        triggers { githubPush() }
        wrappers {
            timeout {
                absolute(75)
            }
            timestamps()
            colorizeOutput('gnome-terminal')
            sshAgent('jenkins-worker')
            credentialsBinding {
                string('AWS_ACCESS_KEY_ID', 'DB_CACHE_ACCESS_KEY_ID')
                string('AWS_SECRET_ACCESS_KEY', 'DB_CACHE_SECRET_ACCESS_KEY')
            }
        }

        Map <String, String> predefinedPropsMap  = [:]
        predefinedPropsMap.put('GIT_SHA', '${GIT_COMMIT}')
        predefinedPropsMap.put('GITHUB_ORG', 'edx')
        predefinedPropsMap.put('CONTEXT', jobConfig.context)
        predefinedPropsMap.put('GITHUB_REPO', jobConfig.repoName)
        predefinedPropsMap.put('TARGET_URL', JENKINS_PUBLIC_BASE_URL +
                                  'job/' + jobConfig.jobName + '/${BUILD_NUMBER}/')
        steps {
            downstreamParameterized JENKINS_PUBLIC_GITHUB_STATUS_PENDING.call(predefinedPropsMap)
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
            downstreamParameterized JENKINS_PUBLIC_GITHUB_STATUS_SUCCESS.call(predefinedPropsMap)
            downstreamParameterized JENKINS_PUBLIC_GITHUB_STATUS_UNSTABLE_OR_WORSE.call(predefinedPropsMap)
            mailer('testeng@edx.org')
            configure GENERAL_SLACK_STATUS()
       }
    }
}

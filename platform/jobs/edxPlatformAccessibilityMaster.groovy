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
    workerLabel: 'js-worker',
    context: 'jenkins/a11y',
    refSpec : '+refs/heads/master:refs/remotes/origin/master',
    defaultBranch : 'master',
    pythonVersion: '3.8',
]

Map privateJobConfig = [
    open: false,
    jobName: 'edx-platform-accessibility-master_private',
    repoName: 'edx-platform-private',
    workerLabel: 'js-worker',
    context: 'jenkins/a11y',
    refSpec : '+refs/heads/security-release:refs/remotes/origin/security-release',
    defaultBranch : 'security-release',
    pythonVersion: '3.8',
]

Map ironwoodJobConfig = [
    open: true,
    jobName: 'ironwood-accessibility-master',
    repoName: 'edx-platform',
    workerLabel: 'ironwood-jenkins-worker',
    context: 'jenkins/ironwood/a11y',
    refSpec : '+refs/heads/open-release/ironwood.master:refs/remotes/origin/open-release/ironwood.master',
    defaultBranch : 'refs/heads/open-release/ironwood.master'
]

List jobConfigs = [
    publicJobConfig,
    privateJobConfig,
    ironwoodJobConfig
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
            archiveArtifacts {
               pattern(JENKINS_PUBLIC_JUNIT_REPORTS)
               pattern('edx-platform*/test_root/log/**/*.png')
               pattern('edx-platform*/test_root/log/**/*.log')
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

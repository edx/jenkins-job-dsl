package platform

import static org.edx.jenkins.dsl.JenkinsPublicConstants.JENKINS_PUBLIC_LOG_ROTATOR

Map wtwPythonJobConfig = [
    open: true,
    jobName: 'edx-platform-python-pipeline-master-wtw',
    repoName: 'edx-platform',
    jenkinsFileDir: 'scripts/Jenkinsfiles',
    jenkinsFileName: 'python',
    branch: 'master',
    context: 'jenkins/python-contexts',
    pythonVersion: '3.8',
]

List jobConfigs = [
    wtwPythonJobConfig,
]

/* Iterate over the job configurations */
jobConfigs.each { jobConfig ->

    // This is the job DSL responsible for creating the main pipeline job.
    pipelineJob(jobConfig.jobName) {

        definition {
            logRotator JENKINS_PUBLIC_LOG_ROTATOR(7)
            environmentVariables(
                REPO_NAME: "${jobConfig.repoName}",
                BRANCH_NAME: "${jobConfig.branch}",
                COLLECT_WHO_TESTS_WHAT: 'true',
                GITHUB_CONTEXT: "${jobConfig.context}",
                PYTHON_VERSION: "${jobConfig.pythonVersion}"
            )

            // Turn off job until we're ready to work on it again; at
            // that time make sure that it's not a blocking check for
            // deploys if this gets reenabled. (Needs to be excluded
            // from check_pr_tests_status.py call.)
            // triggers {
            //     cron('@daily')
            // }

            cpsScm {
                scm {
                    git {
                        extensions {
                            cloneOptions {
                                honorRefspec(true)
                                noTags(true)
                                shallow(true)
                            }
                            sparseCheckoutPaths {
                                sparseCheckoutPaths {
                                    sparseCheckoutPath {
                                        path(jobConfig.jenkinsFileDir)
                                    }
                                }
                            }
                        }
                        remote {
                            credentials('jenkins-worker')
                            github("edx/${jobConfig.repoName}", 'ssh', 'github.com')
                            refspec("+refs/heads/${jobConfig.branch}:refs/remotes/origin/${jobConfig.branch}")
                            branch(jobConfig.branch)
                        }
                    }
                }
                scriptPath(jobConfig.jenkinsFileDir + '/' + jobConfig.jenkinsFileName)
            }
        }
    }
}

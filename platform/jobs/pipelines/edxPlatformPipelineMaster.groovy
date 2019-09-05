package platform

import static org.edx.jenkins.dsl.JenkinsPublicConstants.GENERAL_PRIVATE_JOB_SECURITY
import static org.edx.jenkins.dsl.JenkinsPublicConstants.JENKINS_PUBLIC_LOG_ROTATOR
import static org.edx.jenkins.dsl.JenkinsPublicConstants.GHPRB_CANCEL_BUILDS_ON_UPDATE

Map publicBokchoyJobConfig = [
    open: true,
    jobName: 'edx-platform-bokchoy-pipeline-master',
    repoName: 'edx-platform',
    jenkinsFileDir: 'scripts/Jenkinsfiles',
    jenkinsFileName: 'bokchoy',
    branch: 'master',
    context: 'jenkins/bokchoy',
    pythonVersion: '2.7',
]

Map privateBokchoyJobConfig = [
    open: false,
    jobName: 'edx-platform-bokchoy-pipeline-master_private',
    repoName: 'edx-platform-private',
    jenkinsFileDir: 'scripts/Jenkinsfiles',
    jenkinsFileName: 'bokchoy',
    branch: 'security-release',
    context: 'jenkins/bokchoy',
    pythonVersion: '2.7',
]

Map ironwoodBokchoyJobConfig = [
    open: true,
    jobName: 'ironwood-bokchoy-pipeline-master',
    repoName: 'edx-platform',
    jenkinsFileDir: 'scripts/Jenkinsfiles',
    jenkinsFileName: 'bokchoy',
    branch: 'open-release/ironwood.master',
    context: 'jenkins/ironwood/bokchoy',
    pythonVersion: '2.7',
]

Map ironwoodLettuceJobConfig = [
    open: true,
    jobName: 'ironwood-lettuce-pipeline-master',
    repoName: 'edx-platform',
    jenkinsFileDir: 'scripts/Jenkinsfiles',
    jenkinsFileName: 'lettuce',
    branch: 'open-release/ironwood.master',
    context: 'jenkins/ironwood/lettuce',
    pythonVersion: '2.7',
]

Map publicPythonJobConfig = [
    open: true,
    jobName: 'edx-platform-python-pipeline-master',
    repoName: 'edx-platform',
    jenkinsFileDir: 'scripts/Jenkinsfiles',
    jenkinsFileName: 'python',
    branch: 'master',
    context: 'jenkins/python',
    pythonVersion: '2.7',
]

Map privatePythonJobConfig = [
    open: false,
    jobName: 'edx-platform-python-pipeline-master_private',
    repoName: 'edx-platform-private',
    jenkinsFileDir: 'scripts/Jenkinsfiles',
    jenkinsFileName: 'python',
    branch: 'security-release',
    context: 'jenkins/python',
    pythonVersion: '2.7',
]

Map ironwoodPythonJobConfig = [
    open: true,
    jobName: 'ironwood-python-pipeline-master',
    repoName: 'edx-platform',
    jenkinsFileDir: 'scripts/Jenkinsfiles',
    jenkinsFileName: 'python',
    branch: 'open-release/ironwood.master',
    context: 'jenkins/ironwood/python',
    pythonVersion: '2.7',
]

Map publicQualityJobConfig = [
    open: true,
    jobName: 'edx-platform-quality-pipeline-master',
    repoName: 'edx-platform',
    jenkinsFileDir: 'scripts/Jenkinsfiles',
    jenkinsFileName: 'quality',
    branch: 'master',
    context: 'jenkins/quality',
    pythonVersion: '2.7',
]

Map privateQualityJobConfig = [
    open: false,
    jobName: 'edx-platform-quality-pipeline-master_private',
    repoName: 'edx-platform-private',
    jenkinsFileDir: 'scripts/Jenkinsfiles',
    jenkinsFileName: 'quality',
    branch: 'security-release',
    context: 'jenkins/quality',
    pythonVersion: '2.7',
]

Map ironwoodQualityJobConfig = [
    open: true,
    jobName: 'ironwood-quality-pipeline-master',
    repoName: 'edx-platform',
    jenkinsFileDir: 'scripts/Jenkinsfiles',
    jenkinsFileName: 'quality',
    branch: 'open-release/ironwood.master',
    context: 'jenkins/ironwood/quality',
    pythonVersion: '2.7',
]

List jobConfigs = [
    publicBokchoyJobConfig,
    privateBokchoyJobConfig,
    ironwoodBokchoyJobConfig,
    ironwoodLettuceJobConfig,
    publicPythonJobConfig,
    privatePythonJobConfig,
    ironwoodPythonJobConfig,
    publicQualityJobConfig,
    privateQualityJobConfig,
    ironwoodQualityJobConfig
]

/* Iterate over the job configurations */
jobConfigs.each { jobConfig ->

    // This is the job DSL responsible for creating the main pipeline job.
    pipelineJob(jobConfig.jobName) {

        definition {

            if (!jobConfig.open.toBoolean()) {
                authorization GENERAL_PRIVATE_JOB_SECURITY()
            }
            logRotator JENKINS_PUBLIC_LOG_ROTATOR(7)
            environmentVariables(
                REPO_NAME: "${jobConfig.repoName}",
                BRANCH_NAME: "${jobConfig.branch}",
                GITHUB_CONTEXT: "${jobConfig.context}",
                PYTHON_VERSION: "${jobConfig.pythonVersion}"
            )

            triggers {
                githubPush()
            }

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

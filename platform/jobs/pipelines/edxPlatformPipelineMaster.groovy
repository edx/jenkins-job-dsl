package platform

import static org.edx.jenkins.dsl.JenkinsPublicConstants.JENKINS_PUBLIC_LOG_ROTATOR

Map publicBokchoyJobConfig = [
    jobName: 'edx-platform-bokchoy-pipeline-master',
    jenkinsFileDir: 'scripts/Jenkinsfiles',
    jenkinsFileName: 'bokchoy'
]

Map publicLettuceJobConfig = [
    jobName: 'edx-platform-lettuce-pipeline-master',
    jenkinsFileDir: 'scripts/Jenkinsfiles',
    jenkinsFileName: 'lettuce'
]

Map publicPythonJobConfig = [
    jobName: 'edx-platform-python-pipeline-master',
    jenkinsFileDir: 'scripts/Jenkinsfiles',
    jenkinsFileName: 'python'
]

Map publicQualityJobConfig = [
    jobName: 'edx-platform-quality-pipeline-master',
    jenkinsFileDir: 'scripts/Jenkinsfiles',
    jenkinsFileName: 'quality'
]

List jobConfigs = [
    publicBokchoyJobConfig,
    publicLettuceJobConfig,
    publicPythonJobConfig,
    publicQualityJobConfig
]

/* Iterate over the job configurations */
jobConfigs.each { jobConfig ->

    // This is the job DSL responsible for creating the main pipeline job.
    pipelineJob(jobConfig.jobName) {

        definition {

            logRotator JENKINS_PUBLIC_LOG_ROTATOR(7)

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
                            github('raccoongang/edx-platform', 'ssh', 'github.com')
                            refspec('+refs/heads/master:refs/remotes/origin/master')
                            branch('youngstrom/add-bokchoy-jenkinsfile')
                        }
                    }
                }
                scriptPath(jobConfig.jenkinsFileDir + '/' + jobConfig.jenkinsFileName)
            }
        }
    }
}

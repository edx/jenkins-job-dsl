package platform

import org.yaml.snakeyaml.Yaml
import static org.edx.jenkins.dsl.JenkinsPublicConstants.JENKINS_PUBLIC_LOG_ROTATOR
import static org.edx.jenkins.dsl.JenkinsPublicConstants.JENKINS_PUBLIC_TEAM_SECURITY

// This is the job DSL responsible for creating the main pipeline job.
pipelineJob('edx-platform-quality-pipeline-master') {

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
                                    path('scripts/Jenkinsfiles')
                                }
                            }
                        }
                    }
                    remote {
                        credentials('jenkins-worker')
                        github('edx/edx-platform', 'ssh', 'github.com')
                        refspec('+refs/heads/youngstrom/add-quality-jenkinsfile:refs/remotes/origin/youngstrom/add-quality-jenkinsfile')
                        branch('youngstrom/add-quality-jenkinsfile')
                    }
                }
            }
            scriptPath('scripts/Jenkinsfiles/quality')
        }
    }
}

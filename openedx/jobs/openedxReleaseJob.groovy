package openedx

import static org.edx.jenkins.dsl.JenkinsPublicConstants.JENKINS_PUBLIC_LOG_ROTATOR

// This is the job DSL responsible for creating the main pipeline job.
pipelineJob('openedx-release-ci') {

    definition {

        parameters {
            stringParam('OPENEDX_RELEASE_NAME', 'test-release',
                        'open-release/${OPENEDX_RELEASE_NAME}')
            choiceParam('DELETE_OR_KEEP', ['delete', 'keep'],
                        'What should we do with the new release branches')

        }

        logRotator JENKINS_PUBLIC_LOG_ROTATOR(7)

        cpsScm {
            scm {
                git {
                    branch('master')
                    remote {
                        credentials('jenkins-worker')
                        github('edx/repo-tools', 'ssh', 'github.com')
                        refspec('+refs/heads/master:refs/remotes/origin/master')
                    }
                }
            }
            scriptPath('Jenkinsfiles/openedx-release-ci')
        }

    }
}

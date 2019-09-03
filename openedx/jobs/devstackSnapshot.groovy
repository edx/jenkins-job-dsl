package openedx

import static org.edx.jenkins.dsl.JenkinsPublicConstants.JENKINS_PUBLIC_LOG_ROTATOR

// This is the job DSL responsible for creating the main pipeline job for
// building devstack snapshots for offline installation during conferences, etc.
pipelineJob('devstack-snapshot') {

    definition {

        parameters {
            stringParam('BRANCH', 'master',
                        'The branch of the devstack repository which should be used to build the snapshot')
        }

        logRotator JENKINS_PUBLIC_LOG_ROTATOR(7)

        triggers {
            cron('@weekly')
        }

        cpsScm {
            scm {
                git {
                    branch('${BRANCH}')
                    remote {
                        credentials('jenkins-worker')
                        extensions {
                            relativeTargetDirectory('devstack')
                        }
                        github('edx/devstack', 'ssh', 'github.com')
                        refspec('+refs/heads/${BRANCH}:refs/remotes/origin/${BRANCH}')
                    }
                }
            }
            scriptPath('devstack/scripts/Jenkinsfiles/snapshot')
        }

    }
}

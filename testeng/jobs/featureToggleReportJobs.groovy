import static org.edx.jenkins.dsl.JenkinsPublicConstants.GENERAL_PRIVATE_JOB_SECURITY
import static org.edx.jenkins.dsl.JenkinsPublicConstants.JENKINS_PUBLIC_LOG_ROTATOR

pipelineJob('code-annotation-report-generator') {

    definition {

        authorization GENERAL_PRIVATE_JOB_SECURITY()
        parameters {
            stringParam(
                'BRANCH', 'master',
                'the branch of edx-toggles to check out'
            )
            stringParam(
                'ANNOTATION_REPORT_BUCKET', '',
                'name of s3 bucket to write annotation reports to'
            )
        }

        logRotator JENKINS_PUBLIC_LOG_ROTATOR(7)

        cpsScm {
            scm {
                git {
                    branch('${BRANCH}')
                    remote {
                        credentials('jenkins-worker')
                        extensions {
                            relativeTargetDirectory('edx-toggles')
                        }
                        github('edx/edx-toggles', 'ssh', 'github.com')
                        refspec('+refs/heads/${BRANCH}:refs/remotes/origin/${BRANCH}')
                    }
                }
            }
            scriptPath('edx-toggles/Jenkinsfiles/code-annotation-report-generator')
        }

    }
}

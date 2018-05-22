package testeng

import static org.edx.jenkins.dsl.JenkinsPublicConstants.GENERAL_PRIVATE_JOB_SECURITY
import static org.edx.jenkins.dsl.JenkinsPublicConstants.JENKINS_PUBLIC_MASKED_PASSWORD
import static org.edx.jenkins.dsl.JenkinsPublicConstants.JENKINS_PUBLIC_LOG_ROTATOR
import static org.edx.jenkins.dsl.JenkinsPublicConstants.JENKINS_PUBLIC_HIPCHAT

job('edx-platform-test-notifier') {

    description('Alert developers when edx-platform PR tests are completed.')

    authorization GENERAL_PRIVATE_JOB_SECURITY()

    parameters {
        stringParam('PR_NUMBER', null,
                    'edx-platform PR number to comment')
    }

    logRotator JENKINS_PUBLIC_LOG_ROTATOR()
    label('flow-worker-bokchoy || flow-worker-python || flow-worker-quality || flow-worker-lettuce')
    concurrentBuild()

    scm {
        git {
            remote {
                url('https://github.com/edx/testeng-ci.git')
            }
            branch('*/master')
            browser()
        }
    }

    wrappers {
        timestamps()
        credentialsBinding {
            string('GITHUB_TOKEN', 'GITHUB_STATUS_OAUTH_TOKEN')
        }
    }

    steps {
        shell(readFileFromWorkspace('testeng/resources/edx-platform-test-notifier.sh'))
    }

    publishers {
        mailer('testeng@edx.org')
        hipChatNotifier JENKINS_PUBLIC_HIPCHAT('')
    }
}

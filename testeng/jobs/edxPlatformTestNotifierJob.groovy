package testeng

import static org.edx.jenkins.dsl.JenkinsPublicConstants.GENERAL_PRIVATE_JOB_SECURITY
import static org.edx.jenkins.dsl.JenkinsPublicConstants.JENKINS_PUBLIC_MASKED_PASSWORD
import static org.edx.jenkins.dsl.JenkinsPublicConstants.JENKINS_PUBLIC_LOG_ROTATOR
import static org.edx.jenkins.dsl.JenkinsPublicConstants.GENERAL_SLACK_STATUS

job('edx-platform-test-notifier') {

    description('Alert developers when edx-platform PR tests are completed.')

    authorization GENERAL_PRIVATE_JOB_SECURITY()

    parameters {
        stringParam('REPO', "edx-platform",
                    'Github repository of PR')
        stringParam('PR_NUMBER', null,
                    'PR number')
    }

    logRotator JENKINS_PUBLIC_LOG_ROTATOR()
    label('coverage-worker || jenkins-worker')
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
        buildName('#${BUILD_NUMBER} Platform PR: ${ENV,var="PR_NUMBER"}')
    }

    steps {
        shell(readFileFromWorkspace('testeng/resources/edx-platform-test-notifier.sh'))
    }

    publishers {
        mailer('testeng@edx.org')
        configure GENERAL_SLACK_STATUS()
    }
}

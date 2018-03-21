package testeng

import static org.edx.jenkins.dsl.JenkinsPublicConstants.GENERAL_PRIVATE_JOB_SECURITY
import static org.edx.jenkins.dsl.JenkinsPublicConstants.JENKINS_PUBLIC_MASKED_PASSWORD
import static org.edx.jenkins.dsl.JenkinsPublicConstants.JENKINS_PUBLIC_LOG_ROTATOR

job('edx-platform-test-notifier') {

    description('Trigger firefox builds on a cron')

    authorization GENERAL_PRIVATE_JOB_SECURITY()

    logRotator JENKINS_PUBLIC_LOG_ROTATOR()
    label('flow-worker-bokchoy || flow-worker-python || flow-worker-quality || flow-worker-lettuce')
    concurrentBuild()

    scm {
        git {
            remote {
                url('https://github.com/edx/testeng-ci.git')
            }
            branch('youngstrom/trigger-firefox')
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
        shell(readFileFromWorkspace('testeng/resources/trigger-firefox.sh'))
    }
}

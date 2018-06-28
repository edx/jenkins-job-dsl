package testeng

import static org.edx.jenkins.dsl.JenkinsPublicConstants.GENERAL_PRIVATE_JOB_SECURITY
import static org.edx.jenkins.dsl.JenkinsPublicConstants.JENKINS_PUBLIC_MASKED_PASSWORD
import static org.edx.jenkins.dsl.JenkinsPublicConstants.JENKINS_PUBLIC_LOG_ROTATOR
import static org.edx.jenkins.dsl.JenkinsPublicConstants.JENKINS_PUBLIC_HIPCHAT

job('gather-codecov-metrics') {

    description('Gather metrics on how reliably codecov is posting statuses on pull requests')

    authorization GENERAL_PRIVATE_JOB_SECURITY()

    logRotator JENKINS_PUBLIC_LOG_ROTATOR()
    label('coverage-worker')
    concurrentBuild(false)

    triggers {
        cron('H * * * *')
    }

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
        shell(readFileFromWorkspace('testeng/resources/gather-codecov-metrics.sh'))
    }

    publishers {
        mailer('testeng@edx.org')
        hipChatNotifier JENKINS_PUBLIC_HIPCHAT('')
        archiveArtifacts {
            pattern('codecov_metrics.json')
        }
    }
}

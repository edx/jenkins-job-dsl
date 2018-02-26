package testeng

import static org.edx.jenkins.dsl.JenkinsPublicConstants.JENKINS_PUBLIC_LOG_ROTATOR

job('count-workers') {

    logRotator JENKINS_PUBLIC_LOG_ROTATOR()
    label('master')
    concurrentBuild(false)

    scm {
        git {
            remote {
                url('https://github.com/edx/testeng-ci.git')
            }
            branch('*/master')
            browser()
        }
    }

    triggers{
        cron('*/5 * * * *')
    }

    wrappers {
        timestamps()
    }

    steps {
        shell(readFileFromWorkspace('testeng/resources/count-workers.sh'))
    }

    publishers {
        mailer('testeng@edx.org')
    }
}

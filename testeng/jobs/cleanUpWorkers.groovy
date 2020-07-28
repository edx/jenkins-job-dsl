package testeng

job('clean-up-workers') {

    logRotator {
        daysToKeep(10)
        numToKeep(25)
    }

    label('micro-worker')

    scm {
        git {
            remote {
                url('https://github.com/edx/testeng-ci.git')
            }
            branch('*/master')
            browser()
        }
    }

    triggers {
        cron('H H/6 * * *')
    }

    wrappers {
        timestamps()
    }

    steps {
        systemGroovyScriptFile('jenkins/admin-scripts/delete-old-workers.groovy')
    }

    publishers {
        mailer('testeng@edx.org')
    }
}

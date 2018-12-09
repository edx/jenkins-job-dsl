package testeng

job('travis-counts') {

    logRotator {
        daysToKeep(5)
        numToKeep(201)
    }
    concurrentBuild(true)
    label('coverage-worker')
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
        cron("H/10 * * * *")
    }

    wrappers {
        timestamps()
    }

    steps {
       shell(readFileFromWorkspace('testeng/resources/travis-counts.sh'))
     }
}

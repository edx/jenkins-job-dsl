package testeng

job('travis-job-counts') {

    logRotator {
        daysToKeep(5)
        numToKeep(200)
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
        cron('H/10 * * * *')
    }

    wrappers {
        timestamps()
    }

    environmentVariables {
        env('PYTHON_VERSION', '3.5')
    }

    steps {
       shell(readFileFromWorkspace('testeng/resources/travis-job-counts.sh'))
     }
}

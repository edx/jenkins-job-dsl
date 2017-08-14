package testeng

job('oep2-report') {

    description('Generate a report describing OEP-2 compliance accross edX repos')
    authorization {
        blocksInheritance(true)
        permissionAll('edx')
        permission('hudson.model.Item.Discover', 'anonymous')
    }
    concurrentBuild(false)
    label('jenkins-worker')
    scm {
        git {
            remote {
                url('https://github.com/edx/repo-tools')
            }
            branch('*/master')
            browser()
        }
    }
    triggers {
        cron('@midnight')
    }
    wrappers {
        timestamps()
        sshAgent('jenkins')
        credentialsBinding {
           file('OEP_REPORT_TOKEN', 'GITHUB_STATUS_OAUTH_TOKEN')
        }
    }
    steps {
        virtualenv {
            name('oep-venv')
            nature('shell')
            clear(true)
            command(readFileFromWorkspace('testeng/resources/create-oep-report.sh'))
        }
    }
    publishers {
        archiveJunit('oep2-report.xml') {
            retainLongStdout(false)
        }
        publishHtml {
            report('.') {
                allowMissing(true)
                alwaysLinkToLastBuild(false)
                keepAll(true)
                reportFiles('oep2.report.html')
                reportName('OEP 2 Report')
            }
        }
    }

}

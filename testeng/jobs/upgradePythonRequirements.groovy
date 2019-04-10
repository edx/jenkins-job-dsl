package testeng

Map edxPlatform = [
    org: 'edx',
    repoName: 'edx-platform',
    cronValue: '@daily'
]

List jobConfigs = [
    edxPlatform
]

/* Iterate over the job configurations */
jobConfigs.each { jobConfig ->

    job("${jobConfig.repoName}-upgrade-python-requirements") {

        logRotator {
            daysToKeep(14)
        }
        concurrentBuild(false)
        label('jenkins-worker')
        environmentVariables(
            REPO_NAME: "${jobConfig.repoName}",
            ORG: "${jobConfig.org}"
        )
        multiscm {
            git {
                remote {
                    url("https://github.com/edx/${jobConfig.repoName}.git")
                }
                branch('master')
                extensions {
                    cleanBeforeCheckout()
                    relativeTargetDirectory("${jobConfig.repoName}")
                }
            }
            git {
                remote {
                    url('https://github.com/edx/testeng-ci.git')
                }
                branch('youngstrom/update-github-scripts')
                extensions {
                    cleanBeforeCheckout()
                    relativeTargetDirectory('testeng-ci')
                }
            }
        }
        triggers {
            cron("${jobConfig.cronValue}")
        }

        wrappers {
            timeout {
                absolute(30)
            }
            credentialsBinding {
                string('GITHUB_TOKEN', 'GITHUB_REQUIREMENTS_BOT_TOKEN')
                string('GITHUB_USER_EMAIL', 'GITHUB_REQUIREMENTS_BOT_EMAIL')
            }
            timestamps()
        }

        steps {
           shell(readFileFromWorkspace('testeng/resources/upgrade-python-requirements.sh'))
        }
    }

}

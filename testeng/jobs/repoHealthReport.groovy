package testeng

def pytest_repo_health_gitURL = 'git@github.com:edx/pytest-repo-health.git'
def edx_repo_health_gitURL = 'git@github.com:edx/edx-repo-health.git'
def destination_repo_health_gitURL = "git@github.com:edx/repo-tools-data.git"
def githubUserReviewers = []
def githubTeamReviewers = ['platform-core', 'arch-bom']


job('repo-health-report') {

    description('Generate a report listing repository structure standard compliance accross edX repos')
    parameters {
        stringParam('GITHUB_REPO_URL', 'https://github.com/edx/edx-platform',
                    'Github repo url on which to run pytest-repo-health checks')
    }
    concurrentBuild(false)
    environmentVariables(
            PR_USER_REVIEWERS: githubUserReviewers.join(","),
            PR_TEAM_REVIEWERS: githubTeamReviewers.join(",")
        )
    multiscm {
        git {
            remote {
                url(pytest_repo_health_gitURL)
            }
            branch('master')
            browser()
            extensions {
                cleanAfterCheckout()
                relativeTargetDirectory('pytest-repo-health')
            }
        }
        git {
            remote {
                url(edx_repo_health_gitURL )
            }
            branch('master')
            browser()
            extensions {
                cleanAfterCheckout()
                relativeTargetDirectory('edx-repo-health')
            }
        }
        git {
                remote {
                    url('https://github.com/edx/testeng-ci.git')
                }
                branch('*/msingh/repo_health')
                extensions {
                    cleanBeforeCheckout()
                    relativeTargetDirectory('testeng-ci')
                }
            }
        git {
            remote {
                credentials('jenkins-worker')
                url('${GITHUB_REPO_URL}')
            }
            branch('*/master')
            browser()
            extensions {
                cleanAfterCheckout()
                relativeTargetDirectory('target_repo')
            }
        }
        git {
            remote {
                credentials('jenkins-worker')
                url(destination_repo_health_gitURL )
            }
            branch('*/master')
            browser()
            extensions {
                cleanAfterCheckout()
                relativeTargetDirectory('data_repo')
            }
        }
    }
    triggers {
        cron('@midnight')
    }
    steps{
        shell(readFileFromWorkspace('testeng/resources/create-repo-health-report.sh'))
    }

}

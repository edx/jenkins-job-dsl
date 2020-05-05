/*
    This job requires the folder "RepoHealth" to exist
*/


package testeng

def pytest_repo_health_gitURL = 'git@github.com:edx/pytest-repo-health.git'
def edx_repo_health_gitURL = 'git@github.com:edx/edx-repo-health.git'
def destination_repo_health_gitURL = "git@github.com:edx/repo-tools-data.git"
def githubUserReviewers = []
def githubTeamReviewers = ['git@github.com:edx/platform-core', 'arch-bom']
List targetRepos = ['pytest-repo-health',
                    'edx-platform',
                    'edx-user-state-client',
                    'edx-postman-config',
                    'journals-frontend']

targetRepos.each { target_repo ->

    job("RepoHealth/${target_repo}") {
        description('Generate a report listing repository structure standard compliance accross edX repos')
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
                    credentials('edx-secure')
                    url("git@github.com:edx/${target_repo}.git")
                }
                branch('*/master')
                browser()
                extensions {
                    wipeOutWorkspace()
                    relativeTargetDirectory('target_repo')
                }
            }
        }
        triggers {
            scm('@daily')
        }
        wrappers {
                timeout {
                    absolute(30)
                }
                timestamps()
            }
/*
        steps{
            shell(readFileFromWorkspace('resources/create-repo-health-report.sh'))
        }
*/

    }
}

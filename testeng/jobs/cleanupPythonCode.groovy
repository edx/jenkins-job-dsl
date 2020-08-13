defaultBranch = 'master'

org = 'edx'

githubUserReviewers = ['mraarif']

githubTeamReviewers = ['arbi-bom']

emails = ['arbi-bom@edx.org']


job('cleanup-python-code') {

    parameters {
            stringParam('repoName', null, 'Name of the target repository')
            choiceParam('pythonVersion',['3.8', '3.5', '2.7'], 'Version of python to use')
            stringParam('packages', '', 'Comma separated list of packages to install')
            stringParam('scripts', '', 'Comma separated list of scripts to run')
        }

        repoName = '$repoName'
        pythonVersion = '$pythonVersion'
        packagesToInstall = '$packages'
        scriptsToRun = '$scripts'

        environmentVariables(
              REPO_NAME: repoName,
              ORG: org,
              PACKAGES: packagesToInstall,
              SCRIPTS: scriptsToRun,
              PYTHON_VERSION: pythonVersion,
              PR_USER_REVIEWERS: githubUserReviewers.join(','),
              PR_TEAM_REVIEWERS: githubTeamReviewers.join(',')
         )

        multiscm {
            git {
                remote {
                    credentials('jenkins-worker')
                    url("git@github.com:edx/${repoName}.git")
                }
                branch('master')
                extensions {
                    cleanBeforeCheckout()
                    relativeTargetDirectory(repoName)
                }
            }
            git {
                remote {
                    url('https://github.com/edx/testeng-ci.git')
                }
                branch('master')
                extensions {
                    cleanBeforeCheckout()
                    relativeTargetDirectory('testeng-ci')
                }
            }
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
            shell(readFileFromWorkspace('testeng/resources/cleanup-python-code-create-pr.sh'))
        }
}

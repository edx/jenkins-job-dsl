org = 'edx'

githubUserReviewers = ['mraarif']

githubTeamReviewers = ['arbi-bom']

job('cleanup-python-code') {

    parameters {
        stringParam('repoNames', null, 'Comma separated list of names of (public) target repositories')
        choiceParam('pythonVersion', ['3.8', '3.5', '2.7'], 'Version of python to use')
        stringParam('packages', '', 'Comma separated list of packages to install')
        stringParam('scripts', '', 'Comma separated list of scripts to run')
    }

    repoNames = '${repoNames}'
    pythonVersion = '${pythonVersion}'
    packagesToInstall = '${packages}'
    scriptsToRun = '${scripts}'

    environmentVariables(
            REPO_NAMES: repoNames,
            ORG: org,
            PACKAGES: packagesToInstall,
            SCRIPTS: scriptsToRun,
            PYTHON_VERSION: pythonVersion,
            PR_USER_REVIEWERS: githubUserReviewers.join(','),
            PR_TEAM_REVIEWERS: githubTeamReviewers.join(',')
    )

    scm {
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

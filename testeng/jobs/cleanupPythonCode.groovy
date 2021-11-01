org = 'edx'

githubUserReviewers = ['mraarif']

githubTeamReviewers = ['arbi-bom']

job('cleanup-python-code') {

    parameters {
        stringParam('repoNames', null, 'Comma or space separated list of names of (public) target repositories in $org org')
        choiceParam('pythonVersion', ['3.8', '3.5', '2.7'], 'Version of python to use')
        stringParam('packages', '', 'Comma or space separated list of packages to install (optional)')
        // Use a textarea for multiline input
        textParam('scripts', '', 'Bash script to run')
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
            absolute(120)
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

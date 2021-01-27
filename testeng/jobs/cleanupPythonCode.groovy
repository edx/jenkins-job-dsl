org = 'edx'

githubUserReviewers = ['mraarif']

githubTeamReviewers = ['arbi-bom']

job('cleanup-python-code') {

    parameters {
        stringParam('repoNames', null, 'Comma or space separated list of names of (public) target repositories in $org org')
        choiceParam('pythonVersion', ['3.8', '3.5', '2.7'], 'Version of python to use')
        stringParam('packages', '', 'Comma or space separated list of packages to install')
        stringParam('scripts', '', 'Bash script to run (can separate commands with semicolons)')
        stringParam('prTitle', 'Python Code Cleanup', 'Title for Github PR')
        stringParam('githubTeamReviewers', 'arbi-bom', 'Github team(s) to tag for PR review (comma)')
        stringParam('githubUserReviewers', 'mraarif', 'Github user(s) to tag for PR review')
    }

    repoNames = '${repoNames}'
    pythonVersion = '${pythonVersion}'
    packagesToInstall = '${packages}'
    scriptsToRun = '${scripts}'
    prTitle = '${prTitle}'
    githubTeamReviewers = '${githubTeamReviewers}'
    githubUserReviewers = '${githubUserReviewers}'

    environmentVariables(
            REPO_NAMES: repoNames,
            ORG: org,
            PACKAGES: packagesToInstall,
            SCRIPTS: scriptsToRun,
            PYTHON_VERSION: pythonVersion,
            PR_USER_REVIEWERS: githubUserReviewers,
            PR_TEAM_REVIEWERS: githubTeamReviewers,
            PR_TITLE: prTitle
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

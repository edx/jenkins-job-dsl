githubUserReviewers = ['mraarif']

githubTeamReviewers = ['arbi-bom']

job('cleanup-python-code') {

    parameters {
        choiceParam('org', ['edx', 'openedx'], 'Organization in which the repo(s) is/are located')
        stringParam('repoNames', null, 'Comma or space separated list of names of (public) target repositories in $org org')
        choiceParam('pythonVersion', ['3.8', '3.5', '2.7'], 'Version of python to use')
        stringParam('packages', '', 'Comma or space separated list of packages to install (optional)')
        stringParam('title', null, """Commit message (will also be used as PR title).
            See https://github.com/edx/open-edx-proposals/blob/master/oeps/best-practices/oep-0051-bp-conventional-commits.rst
            for more information on best practices for commit messages""")
        textParam('body', '', """Additional information for the PR body. You can add dynamic information to the
        PR body by writing to .git/cleanup-python-code-description in the bash script (optional)""")
        booleanParam('draft', false, 'Create pull request(s) as draft?')
        booleanParam('forceDeleteOldPrs', false, 'Delete old PRs with same branch name?')
        stringParam('branchName',null,'Branch name for changes (default: "cleanup-python-code")')
        // Use a textarea for multiline input
        textParam('scripts', '', 'Bash script to run')
    }
    org = '${org}'
    repoNames = '${repoNames}'
    pythonVersion = '${pythonVersion}'
    packagesToInstall = '${packages}'
    scriptsToRun = '${scripts}'
    commitMessage='${title}'
    prBody='${body}'
    draft='${draft}'
    branchName='${branchName}'
    forceDeleteOldPrs='${forceDeleteOldPrs}'

    environmentVariables(
            REPO_NAMES: repoNames,
            ORG: org,
            PACKAGES: packagesToInstall,
            SCRIPTS: scriptsToRun,
            PYTHON_VERSION: pythonVersion,
            PR_USER_REVIEWERS: githubUserReviewers.join(','),
            PR_TEAM_REVIEWERS: githubTeamReviewers.join(','),
            COMMIT_MESSAGE: commitMessage,
            PR_BODY: prBody,
            DRAFT: draft,
            BRANCH_NAME: branchName,
            FORCE_DELETE_OLD_PRS: forceDeleteOldPrs,
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

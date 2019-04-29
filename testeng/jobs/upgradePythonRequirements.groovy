package testeng

// This script generates multiple jobs to run make upgrade across different repos
// Map exampleConfig = [
//     org: Github organization,
//     repoName: Github repository,
//     cronValue: How often to run the job,
//     githubUserReviewers: Comma separated list of Githuhb users that should be tagged on PR's, e.g.: user1,user2,user3
//     githubTeamReviewers: Comma separated list of Github teams that should be tagged on PR's, e.g.: team1,team2,team3
// ]

Map bokchoy = [
    org: 'edx',
    repoName: 'bok-choy',
    cronValue: '@weekly',
    githubUserReviewers: '',
    githubTeamReviewers: 'testeng'
]

Map completion = [
    org: 'edx',
    repoName: 'completion',
    cronValue: '@weekly',
    githubUserReviewers: 'feanil',
    githubTeamReviewers: 'testeng'
]

Map cookiecutterDjangoApp = [
    org: 'edx',
    repoName: 'cookiecutter-django-app',
    cronValue: '@weekly',
    githubUserReviewers: '',
    githubTeamReviewers: 'testeng'
]

Map devstack = [
    org: 'edx',
    repoName: 'devstack',
    cronValue: '@weekly',
    githubUserReviewers: '',
    githubTeamReviewers: 'testeng'
]

Map djangoConfigModels = [
    org: 'edx',
    repoName: 'django-config-models',
    cronValue: '@weekly',
    githubUserReviewers: 'feanil',
    githubTeamReviewers: 'testeng'
]

Map edxPlatform = [
    org: 'edx',
    repoName: 'edx-platform',
    cronValue: '@daily',
    githubUserReviewers: '',
    githubTeamReviewers: 'testeng'
]

Map testengCI = [
    org: 'edx',
    repoName: 'testeng-ci',
    cronValue: '@weekly',
    githubUserReviewers: '',
    githubTeamReviewers: 'testeng'
]

List jobConfigs = [
    bokchoy,
    completion,
    cookiecutterDjangoApp,
    devstack,
    djangoConfigModels,
    edxPlatform,
    testengCI,
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
            ORG: "${jobConfig.org}",
            PR_USER_REVIEWERS: "${jobConfig.githubUserReviewers}",
            PR_TEAM_REVIEWERS: "${jobConfig.githubTeamReviewers}"
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
                branch('master')
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

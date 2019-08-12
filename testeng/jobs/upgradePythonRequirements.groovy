package testeng

// This script generates multiple jobs to run make upgrade across different repos
// Map exampleConfig = [
//     org: Github organization,
//     repoName: Github repository,
//     cronValue: How often to run the job,
//     githubUserReviewers: List of Github users that should be tagged on PR's, e.g.: ['user1', 'user2']
//     githubTeamReviewers: List of Github teams that should be tagged on PR's, e.g.: ['team1']
//     emails: List of emails that should be notified when job fails, e.g.: ['email1', 'email2']
// ]

Map bokchoy = [
    org: 'edx',
    repoName: 'bok-choy',
    cronValue: '@weekly',
    githubUserReviewers: [],
    githubTeamReviewers: ['testeng'],
    emails: ['testeng@edx.org']
]

Map completion = [
    org: 'edx',
    repoName: 'completion',
    cronValue: '@weekly',
    githubUserReviewers: ['feanil'],
    githubTeamReviewers: ['testeng'],
    emails: ['feanil@edx.org', 'testeng@edx.org']
]

Map cookiecutterDjangoApp = [
    org: 'edx',
    repoName: 'cookiecutter-django-app',
    cronValue: '@weekly',
    githubUserReviewers: [],
    githubTeamReviewers: ['testeng'],
    emails: ['testeng@edx.org']
]

Map courseDiscovery = [
    org: 'edx',
    repoName: 'course-discovery',
    cronValue: '@weekly',
    githubUserReviewers: ['mikix'],
    githubTeamReviewers: ['testeng'],
    emails: ['mterry@edx.org', 'testeng@edx.org']
]

Map credentials = [
    org: 'edx',
    repoName: 'credentials',
    cronValue: '@weekly',
    githubUserReviewers: [],
    githubTeamReviewers: ['masters-neem'],
    emails: ['masters-neem@edx.org']
]

Map devstack = [
    org: 'edx',
    repoName: 'devstack',
    cronValue: '@weekly',
    githubUserReviewers: [],
    githubTeamReviewers: ['testeng'],
    emails: ['testeng@edx.org']
]

Map djangoConfigModels = [
    org: 'edx',
    repoName: 'django-config-models',
    cronValue: '@weekly',
    githubUserReviewers: ['feanil'],
    githubTeamReviewers: ['testeng'],
    emails: ['feanil@edx.org', 'testeng@edx.org']
]

Map djangoUserTasks = [
    org: 'edx',
    repoName: 'django-user-tasks',
    cronValue: '@weekly',
    githubUserReviewers: ['jmbowman'],
    githubTeamReviewers: ['testeng'],
    emails: ['testeng@edx.org']
]

Map ecommerce = [
    org: 'edx',
    repoName: 'ecommerce',
    cronValue: '@weekly',
    githubUserReviewers: [],
    githubTeamReviewers: ['testeng', 'edx/rev-team'],
    emails: ['revenue-squad-alert@edx.opsgenie.net', 'testeng@edx.org']
]

Map edxOrganizations = [
    org: 'edx',
    repoName: 'edx-organizations',
    cronValue: '@weekly',
    githubUserReviewers: ['feanil'],
    githubTeamReviewers: ['testeng'],
    emails: ['feanil@edx.org', 'testeng@edx.org']
]

Map edxPlatform = [
    org: 'edx',
    repoName: 'edx-platform',
    cronValue: '@daily',
    githubUserReviewers: [],
    githubTeamReviewers: ['platform-core', 'testeng'],
    emails: ['testeng@edx.org']
]

Map edxProctoring = [
    org: 'edx',
    repoName: 'edx-proctoring',
    cronValue: '@weekly',
    githubUserReviewers: ['feanil'],
    githubTeamReviewers: ['testeng', 'Masters-dahlia'],
    emails: ['feanil@edx.org', 'testeng@edx.org', 'masters-dahlia@edx.org']
]

Map opaqueKeys = [
    org: 'edx',
    repoName: 'opaque-keys',
    cronValue: '@weekly',
    githubUserReviewers: ['cpennington'],
    githubTeamReviewers: ['platform-core'],
    emails: ['testeng@edx.org']
]

Map registrar = [
    org: 'edx',
    repoName: 'registrar',
    cronValue: '@weekly',
    githubUserReviewers: [],
    githubTeamReviewers: ['masters-neem'],
    emails: ['masters-neem@edx.org']
]

Map testengCI = [
    org: 'edx',
    repoName: 'testeng-ci',
    cronValue: '@weekly',
    githubUserReviewers: [],
    githubTeamReviewers: ['testeng'],
    emails: ['testeng@edx.org']
]

Map xblock = [
    org: 'edx',
    repoName: 'XBlock',
    cronValue: '@weekly',
    githubUserReviewers: ['cpennington'],
    githubTeamReviewers: ['platform-core'],
    emails: ['testeng@edx.org']
]


List jobConfigs = [
    bokchoy,
    completion,
    cookiecutterDjangoApp,
    courseDiscovery,
    credentials,
    devstack,
    djangoConfigModels,
    djangoUserTasks,
    ecommerce,
    edxOrganizations,
    edxPlatform,
    edxProctoring,
    opaqueKeys,
    registrar,
    testengCI,
    xblock,
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
            REPO_NAME: jobConfig.repoName,
            ORG: jobConfig.org,
            PR_USER_REVIEWERS: jobConfig.githubUserReviewers.join(","),
            PR_TEAM_REVIEWERS: jobConfig.githubTeamReviewers.join(",")
        )
        multiscm {
            git {
                remote {
                    url("https://github.com/edx/${jobConfig.repoName}.git")
                }
                branch('master')
                extensions {
                    cleanBeforeCheckout()
                    relativeTargetDirectory(jobConfig.repoName)
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
            cron(jobConfig.cronValue)
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

        publishers {
            mailer(jobConfig.emails.join(" "))
        }
    }

}

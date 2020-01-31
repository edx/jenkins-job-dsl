package testeng

// This script generates multiple jobs to run make upgrade across different repos
// Map exampleConfig = [
//     org: Github organization,
//     repoName: Github repository,
//     pythonVersion: The version of python under which to run pip-compile.  If multiple versions are supported by the repo, use the oldest one
//     cronValue: How often to run the job,
//     githubUserReviewers: List of Github users that should be tagged on PR's, e.g.: ['user1', 'user2']
//     githubTeamReviewers: List of Github teams that should be tagged on PR's, e.g.: ['team1']
//     emails: List of emails that should be notified when job fails, e.g.: ['email1', 'email2']
// ]

Map bokchoy = [
    org: 'edx',
    repoName: 'bok-choy',
    pythonVersion: '2.7',
    cronValue: '@weekly',
    githubUserReviewers: [],
    githubTeamReviewers: ['arch-bom'],
    emails: ['arch-bom@edx.org']
]

Map completion = [
    org: 'edx',
    repoName: 'completion',
    pythonVersion: '2.7',
    cronValue: '@weekly',
    githubUserReviewers: [],
    githubTeamReviewers: ['masters-devs'],
    emails: ['masters-requirements-update@edx.opsgenie.net']
]

Map cookiecutterDjangoApp = [
    org: 'edx',
    repoName: 'cookiecutter-django-app',
    pythonVersion: '3.5',
    cronValue: '@weekly',
    githubUserReviewers: [],
    githubTeamReviewers: ['arch-bom'],
    emails: ['arch-bom@edx.org']
]

Map courseDiscovery = [
    org: 'edx',
    repoName: 'course-discovery',
    pythonVersion: '3.5',
    cronValue: '@weekly',
    githubUserReviewers: ['mikix'],
    githubTeamReviewers: ['course-discovery-admins'],
    emails: ['mterry@edx.org']
]

Map credentials = [
    org: 'edx',
    repoName: 'credentials',
    pythonVersion: '3.5',
    cronValue: '@weekly',
    githubUserReviewers: [],
    githubTeamReviewers: ['masters-devs'],
    emails: ['masters-requirements-update@edx.opsgenie.net']
]

Map devstack = [
    org: 'edx',
    repoName: 'devstack',
    pythonVersion: '2.7',
    cronValue: '@weekly',
    githubUserReviewers: [],
    githubTeamReviewers: ['arch-bom'],
    emails: ['arch-bom@edx.org']
]

Map djangoConfigModels = [
    org: 'edx',
    repoName: 'django-config-models',
    pythonVersion: '2.7',
    cronValue: '@weekly',
    githubUserReviewers: ['feanil'],
    githubTeamReviewers: ['arch-bom'],
    emails: ['arch-bom@edx.org']
]

Map djangoUserTasks = [
    org: 'edx',
    repoName: 'django-user-tasks',
    pythonVersion: '2.7',
    cronValue: '@weekly',
    githubUserReviewers: ['jmbowman'],
    githubTeamReviewers: ['arch-bom'],
    emails: ['arch-bom@edx.org']
]

Map ecommerce = [
    org: 'edx',
    repoName: 'ecommerce',
    pythonVersion: '3.5',
    cronValue: '@weekly',
    githubUserReviewers: [],
    githubTeamReviewers: ['edx/rev-team'],
    emails: ['revenue-squad-alert@edx.opsgenie.net']
]

Map edxGomatic = [
    org: 'edx',
    repoName: 'edx-gomatic',
    pythonVersion: '2.7',
    cronValue: '@weekly',
    githubUserReviewers: [],
    githubTeamReviewers: ['devops'],
    emails: ['devops@edx.org']
]

Map edxNotesApi = [
    org: 'edx',
    repoName: 'edx-notes-api',
    pythonVersion: '3.5',
    cronValue: '@weekly',
    githubUserReviewers: [],
    githubTeamReviewers: ['devops'],
    emails: ['devops@edx.org']
]

Map edxOrganizations = [
    org: 'edx',
    repoName: 'edx-organizations',
    pythonVersion: '2.7',
    cronValue: '@weekly',
    githubUserReviewers: ['feanil'],
    githubTeamReviewers: ['arch-bom'],
    emails: ['arch-bom@edx.org']
]

Map edxPlatform = [
    org: 'edx',
    repoName: 'edx-platform',
    pythonVersion: '3.5',
    cronValue: '@daily',
    githubUserReviewers: [],
    githubTeamReviewers: ['platform-core', 'arch-bom'],
    emails: ['arch-bom@edx.org']
]

Map edxProctoring = [
    org: 'edx',
    repoName: 'edx-proctoring',
    pythonVersion: '3.5',
    cronValue: '@weekly',
    githubUserReviewers: ['feanil'],
    githubTeamReviewers: ['masters-devs'],
    emails: ['masters-requirements-update@edx.opsgenie.net']
]

Map edxSphinxTheme = [
    org: 'edx',
    repoName: 'edx-sphinx-theme',
    pythonVersion: '2.7',
    cronValue: '@weekly',
    githubUserReviewers: ['jmbowman'],
    githubTeamReviewers: ['arch-bom'],
    emails: ['arch-bom@edx.org']
]

Map opaqueKeys = [
    org: 'edx',
    repoName: 'opaque-keys',
    pythonVersion: '2.7',
    cronValue: '@weekly',
    githubUserReviewers: ['cpennington'],
    githubTeamReviewers: ['platform-core', 'arch-bom'],
    emails: ['arch-bom@edx.org']
]

Map portalDesigner = [
    org: 'edx',
    repoName: 'portal-designer',
    pythonVersion: '3.5',
    cronValue: '@weekly',
    githubUserReviewers: [],
    githubTeamReviewers: ['masters-devs'],
    emails: ['masters-requirements-update@edx.opsgenie.net']
]

Map registrar = [
    org: 'edx',
    repoName: 'registrar',
    pythonVersion: '3.6',
    cronValue: '@weekly',
    githubUserReviewers: [],
    githubTeamReviewers: ['masters-devs'],
    emails: ['masters-requirements-update@edx.opsgenie.net']
]

Map testengCI = [
    org: 'edx',
    repoName: 'testeng-ci',
    pythonVersion: '2.7',
    cronValue: '@weekly',
    githubUserReviewers: [],
    githubTeamReviewers: ['arch-bom'],
    emails: ['arch-bom@edx.org']
]

Map xblock = [
    org: 'edx',
    repoName: 'XBlock',
    pythonVersion: '2.7',
    cronValue: '@weekly',
    githubUserReviewers: ['cpennington'],
    githubTeamReviewers: ['platform-core', 'arch-bom'],
    emails: ['arch-bom@edx.org']
]

Map xqueue = [
    org: 'edx',
    repoName: 'xqueue',
    pythonVersion: '3.5',
    cronValue: '@weekly',
    githubUserReviewers: [],
    githubTeamReviewers: ['sustaining-team'],
    emails: ['sustaining-escalations@edx.org']
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
    edxGomatic,
    edxNotesApi,
    edxOrganizations,
    edxPlatform,
    edxProctoring,
    edxSphinxTheme,
    opaqueKeys,
    portalDesigner,
    registrar,
    testengCI,
    xblock,
    xqueue,
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
            PYTHON_VERSION: jobConfig.pythonVersion,
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

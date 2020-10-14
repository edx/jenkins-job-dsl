package testeng

// This script generates multiple jobs to run make upgrade across different repos
// Map exampleConfig = [
//     org: Github organization,
//     repoName: Github repository,
//     targetBranch: the default branch of the repository. Ideally, this should be 'main'
//     pythonVersion: The version of python under which to run pip-compile.  If multiple versions are supported by the repo, use the oldest one
//     cronValue: How often to run the job,
//     githubUserReviewers: List of Github users that should be tagged on PR's, e.g.: ['user1', 'user2']
//     githubTeamReviewers: List of Github teams that should be tagged on PR's, e.g.: ['team1'].  IMPORTANT NOTE: the teams must have explicit write access to the repository
//     emails: List of emails that should be notified on completion, e.g.: ['email1', 'email2']
//     alwaysNotify: Boolean, whether we send an email notification even when the job succeeded rather than just on failure (the default)
// ]

// Cron value for once per week, sometime during: midnight-7:59am ET, Monday-Friday (times are in UTC)
// See https://github.com/jenkinsci/jenkins/blob/master/core/src/main/resources/hudson/triggers/TimerTrigger/help-spec.jelly
def cronOffHoursBusinessWeekday = 'H H(4-11) * * H(1-5)'
// Cron value for once per week, sometime during: 5am-09:59am Pakistan Time, Monday-Friday (times are in UTC)
def cronOffHoursBusinessWeekdayLahore = 'H H(0-5) * * H(1-5)'

// Cron value for twice per month sometime during: midnight-7:59am ET, Monday-Friday (times are in UTC)
def cronOffHoursBusinessWeekdayTwiceMonthly = 'H H(4-11) H/14 * H(1-5)'
// same but for Lahore-appropriate times
def cronOffHoursBusinessWeekdayLahoreTwiceMonthly = 'H H(0-5) H/14 * H(1-5)'

// Cron value for daily, sometime during: midnight-7:59am ET, Monday-Friday (times are in UTC)
// See https://github.com/jenkinsci/jenkins/blob/master/core/src/main/resources/hudson/triggers/TimerTrigger/help-spec.jelly
def cronOffHoursBusinessDaily = 'H H(4-11) * * 1-5'
// Cron value for daily, sometime during: 5am-09:59am Pakistan Time, Monday-Friday (times are in UTC)
def cronOffHoursBusinessDailyLahore = 'H H(0-5) * * 1-5'

Map apiDocTools = [
    org: 'edx',
    repoName: 'api-doc-tools',
    targetBranch: "master",
    pythonVersion: '3.5',
    cronValue: cronOffHoursBusinessWeekday,
    githubUserReviewers: [],
    githubTeamReviewers: [],
    emails: ['ned@edx.org']
]

Map authBackends = [
    org: 'edx',
    repoName: 'auth-backends',
    targetBranch: "master",
    pythonVersion: '3.5',
    cronValue: cronOffHoursBusinessWeekdayLahore,
    githubUserReviewers: [],
    githubTeamReviewers: ['arbi-bom'],
    emails: ['arbi-bom@edx.org'],
    alwaysNotify: false
]

Map bokchoy = [
    org: 'edx',
    repoName: 'bok-choy',
    targetBranch: "master",
    pythonVersion: '3.5',
    cronValue: cronOffHoursBusinessWeekdayLahore,
    githubUserReviewers: [],
    githubTeamReviewers: ['arbi-bom'],
    emails: ['arbi-bom@edx.org'],
    alwaysNotify: false
]

Map cc2olx = [
    org: 'edx',
    repoName: 'cc2olx',
    targetBranch: 'master',
    pythonVersion: '3.5',
    cronValue: cronOffHoursBusinessWeekdayTwiceMonthly,
    githubUserReviewers: [],
    githubTeamReviewers: ['masters-devs-cosmonauts'],
    emails: ['masters-requirements-update@edx.opsgenie.net'],
]

Map coachingPlugin = [
    org: 'edx',
    repoName: 'platform-plugin-coaching',
    targetBranch: "master",
    pythonVersion: '3.5',
    cronValue: cronOffHoursBusinessWeekday,
    githubUserReviewers: [],
    githubTeamReviewers: ['edx-aperture'],
    emails: ['aperture-alerts@edx.org']
]

Map configuration = [
    org: 'edx',
    repoName: 'configuration',
    targetBranch: "master",
    pythonVersion: '3.5',
    cronValue: cronOffHoursBusinessWeekday,
    githubUserReviewers: ['fredsmith'],
    githubTeamReviewers: ['devops'],
    emails: ['devops@edx.org'],
     alwaysNotify: false
]

Map completion = [
    org: 'edx',
    repoName: 'completion',
    targetBranch: "master",
    pythonVersion: '3.5',
    cronValue: cronOffHoursBusinessWeekday,
    githubUserReviewers: [],
    githubTeamReviewers: [],  // Reviewer mention unnecessary due to Master's OpsGenie alert.
    emails: ['always-available@edx.opsgenie.net'],
    alwaysNotify: false
]

Map cookiecutters = [
    org: 'edx',
    repoName: 'edx-cookiecutters',
    targetBranch: "master",
    pythonVersion: '3.5',
    cronValue: cronOffHoursBusinessWeekdayLahore,
    githubUserReviewers: [],
    githubTeamReviewers: ['arbi-bom'],
    emails: ['arbi-bom@edx.org'],
    alwaysNotify: false
]

Map courseDiscovery = [
    org: 'edx',
    repoName: 'course-discovery',
    targetBranch: "master",
    pythonVersion: '3.5',
    cronValue: cronOffHoursBusinessWeekday,
    githubUserReviewers: ['mikix'],
    githubTeamReviewers: ['course-discovery-admins'],
    emails: ['mterry@edx.org'],
    alwaysNotify: false
]

Map credentialsRepo = [
    org: 'edx',
    repoName: 'credentials',
    targetBranch: "master",
    pythonVersion: '3.5',
    cronValue: cronOffHoursBusinessWeekdayTwiceMonthly,
    githubUserReviewers: [],
    githubTeamReviewers: ['edx-aperture'],
    emails: ['aperture-alerts@edx.org'],
]

Map demographics = [
    org: 'edx',
    repoName: 'demographics',
    targetBranch: "master",
    pythonVersion: '3.6',
    cronValue: cronOffHoursBusinessWeekday,
    githubUserReviewers: [],
    githubTeamReviewers: ['edx-aperture'],
    emails: ['aperture-alerts@edx.org']
]

Map devstack = [
    org: 'edx',
    repoName: 'devstack',
    targetBranch: "master",
    pythonVersion: '3.5',
    cronValue: cronOffHoursBusinessWeekdayLahore,
    githubUserReviewers: [],
    githubTeamReviewers: ['arbi-bom'],
    emails: ['arbi-bom@edx.org'],
    alwaysNotify: false
]

Map djangoConfigModels = [
    org: 'edx',
    repoName: 'django-config-models',
    targetBranch: "master",
    pythonVersion: '3.5',
    cronValue: cronOffHoursBusinessWeekdayLahore,
    githubUserReviewers: [],
    githubTeamReviewers: ['arbi-bom'],
    emails: ['arbi-bom@edx.org'],
    alwaysNotify: false
]

Map djangoLangPrefMiddleware = [
    org: 'edx',
    repoName: 'django-lang-pref-middleware',
    targetBranch: "master",
    pythonVersion: '3.5',
    cronValue: cronOffHoursBusinessWeekdayLahore,
    githubUserReviewers: [],
    githubTeamReviewers: ['arbi-bom'],
    emails: ['arbi-bom@edx.org']
]

Map djangoUserTasks = [
    org: 'edx',
    repoName: 'django-user-tasks',
    targetBranch: "master",
    pythonVersion: '3.5',
    cronValue: cronOffHoursBusinessWeekdayLahore,
    githubUserReviewers: ['jmbowman'],
    githubTeamReviewers: ['arbi-bom'],
    emails: ['arbi-bom@edx.org'],
    alwaysNotify: false
]

Map ecommerce = [
    org: 'edx',
    repoName: 'ecommerce',
    targetBranch: "master",
    pythonVersion: '3.5',
    cronValue: cronOffHoursBusinessWeekday,
    githubUserReviewers: [],
    githubTeamReviewers: ['ecommerce'],
    emails: ['revenue-tasks@edx.org']
]

Map edxAce = [
    org: 'edx',
    repoName: 'edx-ace',
    targetBranch: "master",
    pythonVersion: '3.5',
    cronValue: cronOffHoursBusinessWeekdayLahore,
    githubUserReviewers: [],
    githubTeamReviewers: ['arbi-bom'],
    emails: ['arbi-bom@edx.org']
]

Map edxAnalyticsDashboard = [
    org: 'edx',
    repoName: 'edx-analytics-dashboard',
    targetBranch: "master",
    pythonVersion: '3.5',
    cronValue: cronOffHoursBusinessWeekday,
    githubUserReviewers: [],
    githubTeamReviewers: ['edx-data-engineering'],
    emails: ['data-engineering@edx.org']
]

Map edxAnalyticsDataApi = [
    org: 'edx',
    repoName: 'edx-analytics-data-api',
    targetBranch: "master",
    pythonVersion: '3.8',
    cronValue: cronOffHoursBusinessWeekday,
    githubUserReviewers: [],
    githubTeamReviewers: ['edx-data-engineering'],
    emails: ['data-engineering@edx.org']
]

Map edxBulkGrades = [
    org: 'edx',
    repoName: 'edx-bulk-grades',
    targetBranch: "master",
    pythonVersion: '3.5',
    cronValue: cronOffHoursBusinessWeekdayTwiceMonthly,
    githubUserReviewers: [],
    githubTeamReviewers: [],  // Reviewer mention unnecessary due to Master's OpsGenie alert.
    emails: ['masters-requirements-update@edx.opsgenie.net'],
    alwaysNotify: true
]

Map edxCeleryutils = [
    org: 'edx',
    repoName: 'edx-celeryutils',
    targetBranch: "master",
    pythonVersion: '3.5',
    cronValue: cronOffHoursBusinessWeekdayLahore,
    githubUserReviewers: [],
    githubTeamReviewers: ['arbi-bom'],
    emails: ['arbi-bom@edx.org'],
    alwaysNotify: false
]

Map edxDjangoUtils = [
    org: 'edx',
    repoName: 'edx-django-utils',
    targetBranch: "master",
    pythonVersion: '3.5',
    cronValue: cronOffHoursBusinessWeekday,
    githubUserReviewers: [],
    githubTeamReviewers: ['arbi-bom'],
    emails: ['arbi-bom@edx.org']
]

Map edxDrfExtensions = [
    org: 'edx',
    repoName: 'edx-drf-extensions',
    targetBranch: "master",
    pythonVersion: '3.5',
    cronValue: cronOffHoursBusinessWeekdayLahore,
    githubUserReviewers: [],
    githubTeamReviewers: ['arbi-bom'],
    emails: ['arbi-bom@edx.org']
]

Map edxE2eTests = [
    org: 'edx',
    repoName: 'edx-e2e-tests',
    targetBranch: "master",
    pythonVersion: '3.5',
    cronValue: cronOffHoursBusinessWeekdayLahore,
    githubUserReviewers: [],
    githubTeamReviewers: ['arbi-bom'],
    emails: ['arbi-bom@edx.org'],
    alwaysNotify: false
]

Map edxEnterprise = [
    org: 'edx',
    repoName: 'edx-enterprise',
    targetBranch: "master",
    pythonVersion: '3.5',
    cronValue: cronOffHoursBusinessWeekday,
    githubUserReviewers: ['georgebabey'],
    githubTeamReviewers: ['business-enterprise-team'],
    emails: ['arbi-bom@edx.org']
]

Map edxGomatic = [
    org: 'edx',
    repoName: 'edx-gomatic',
    targetBranch: "master",
    pythonVersion: '2.7',
    cronValue: cronOffHoursBusinessWeekday,
    githubUserReviewers: [],
    githubTeamReviewers: ['devops'],
    emails: ['devops@edx.org']
]

Map edxNotesApi = [
    org: 'edx',
    repoName: 'edx-notes-api',
    targetBranch: "master",
    pythonVersion: '3.8',
    cronValue: cronOffHoursBusinessWeekday,
    githubUserReviewers: [],
    githubTeamReviewers: ['devops'],
    emails: ['devops@edx.org'],
    alwaysNotify: false
]

Map edxOra2 = [
    org: 'edx',
    repoName: 'edx-ora2',
    targetBranch: "master",
    pythonVersion: '3.5',
    cronValue: cronOffHoursBusinessWeekdayTwiceMonthly,
    githubUserReviewers: [],
    githubTeamReviewers: [],  // Reviewer mention unnecessary due to Master's OpsGenie alert.
    emails: ['masters-requirements-update@edx.opsgenie.net'],
    alwaysNotify: true
]

Map edxOrganizations = [
    org: 'edx',
    repoName: 'edx-organizations',
    targetBranch: "master",
    pythonVersion: '3.5',
    cronValue: cronOffHoursBusinessWeekdayLahore,
    githubUserReviewers: [],
    githubTeamReviewers: ['arbi-bom'],
    emails: ['arbi-bom@edx.org'],
    alwaysNotify: false
]

Map edxPlatform = [
    org: 'edx',
    repoName: 'edx-platform',
    targetBranch: "master",
    pythonVersion: '3.5',
    cronValue: cronOffHoursBusinessDailyLahore,
    githubUserReviewers: [],
    githubTeamReviewers: ['arbi-bom'],
    emails: ['arbi-bom@edx.org'],
    alwaysNotify: false
]

Map edxProctoring = [
    org: 'edx',
    repoName: 'edx-proctoring',
    targetBranch: "master",
    pythonVersion: '3.5',
    cronValue: cronOffHoursBusinessWeekdayTwiceMonthly,
    githubUserReviewers: [],
    githubTeamReviewers: [],  // Reviewer mention unnecessary due to Master's OpsGenie alert.
    emails: ['masters-requirements-update@edx.opsgenie.net'],
    alwaysNotify: true
]

Map edxRbac = [
    org: 'edx',
    repoName: 'edx-rbac',
    targetBranch: "master",
    pythonVersion: '3.5',
    cronValue: cronOffHoursBusinessWeekdayLahore,
    githubUserReviewers: [],
    githubTeamReviewers: ['arbi-bom'],
    emails: ['arbi-bom@edx.org'],
    alwaysNotify: false
]

Map edxRepoHealth = [
    org: 'edx',
    repoName: 'edx-repo-health',
    targetBranch: "master",
    pythonVersion: '3.6',
    cronValue: cronOffHoursBusinessWeekdayLahore,
    githubUserReviewers: [],
    githubTeamReviewers: ['arbi-bom'],
    emails: ['arbi-bom@edx.org'],
    alwaysNotify: false
]

Map edxRestApiClient = [
    org: 'edx',
    repoName: 'edx-rest-api-client',
    targetBranch: "master",
    pythonVersion: '3.5',
    cronValue: cronOffHoursBusinessWeekdayLahore,
    githubUserReviewers: [''],
    githubTeamReviewers: ['arbi-bom'],
    emails: ['arbi-bom@edx.org'],
    alwaysNotify: false
]

Map edxSphinxTheme = [
    org: 'edx',
    repoName: 'edx-sphinx-theme',
    targetBranch: "master",
    pythonVersion: '3.5',
    cronValue: cronOffHoursBusinessWeekdayLahore,
    githubUserReviewers: [''],
    githubTeamReviewers: ['arbi-bom'],
    emails: ['arbi-bom@edx.org'],
    alwaysNotify: false
]

Map edxToggles = [
    org: 'edx',
    repoName: 'edx-toggles',
    targetBranch: "master",
    pythonVersion: '3.5',
    cronValue: cronOffHoursBusinessWeekdayLahore,
    githubUserReviewers: [],
    githubTeamReviewers: ['arbi-bom'],
    emails: ['arbi-bom@edx.org'],
    alwaysNotify: false
]

Map edxVal= [
    org: 'edx',
    repoName: 'edx-val',
    targetBranch: "master",
    pythonVersion: '3.5',
    cronValue: cronOffHoursBusinessWeekday,
    githubUserReviewers: [''],
    githubTeamReviewers: ['sustaining-vulcans'],
    emails: ['sustaining-vulcans@edx.org'],
    alwaysNotify: false
]

Map notifier = [
    org: 'edx',
    repoName: 'notifier',
    targetBranch: "master",
    pythonVersion: '3.5',
    cronValue: cronOffHoursBusinessWeekdayLahore,
    githubUserReviewers: [],
    githubTeamReviewers: ['arbi-bom'],
    emails: ['arbi-bom@edx.org'],
    alwaysNotify: false
]

Map opaqueKeys = [
    org: 'edx',
    repoName: 'opaque-keys',
    targetBranch: "master",
    pythonVersion: '3.5',
    cronValue: cronOffHoursBusinessWeekdayLahore,
    githubUserReviewers: ['cpennington'],
    githubTeamReviewers: ['arbi-bom'],
    emails: ['arbi-bom@edx.org'],
    alwaysNotify: false
]

Map openEdxStats = [
    org: 'edx',
    repoName: 'openedxstats',
    targetBranch: "master",
    pythonVersion: '3.5',
    cronValue: cronOffHoursBusinessWeekday,
    githubUserReviewers: [],
    githubTeamReviewers: [],
    emails: ['ned@edx.org'],
    alwaysNotify: false
]

Map portalDesigner = [
    org: 'edx',
    repoName: 'portal-designer',
    targetBranch: "master",
    pythonVersion: '3.8',
    cronValue: cronOffHoursBusinessWeekdayTwiceMonthly,
    githubUserReviewers: [],
    githubTeamReviewers: [],  // Reviewer mention unnecessary due to Master's OpsGenie alert.
    emails: ['masters-requirements-update@edx.opsgenie.net'],
    alwaysNotify: true
]

Map pytestRepoHealth = [
    org: 'edx',
    repoName: 'pytest-repo-health',
    targetBranch: "master",
    pythonVersion: '3.6',
    cronValue: cronOffHoursBusinessWeekdayLahore,
    githubUserReviewers: [],
    githubTeamReviewers: ['arbi-bom'],
    emails: ['arbi-bom@edx.org'],
    alwaysNotify: false
]

Map registrar = [
    org: 'edx',
    repoName: 'registrar',
    targetBranch: "master",
    pythonVersion: '3.5',
    cronValue: cronOffHoursBusinessWeekdayTwiceMonthly,
    githubUserReviewers: [],
    githubTeamReviewers: [],  // Reviewer mention unnecessary due to Master's OpsGenie alert.
    emails: ['masters-requirements-update@edx.opsgenie.net'],
    alwaysNotify: true
]

Map staffGradedXBlock = [
    org: 'edx',
    repoName: 'staff_graded-xblock',
    targetBranch: "master",
    pythonVersion: '3.5',
    cronValue: cronOffHoursBusinessWeekdayTwiceMonthly,
    githubUserReviewers: [],
    githubTeamReviewers: [],  // Reviewer mention unnecessary due to Master's OpsGenie alert.
    emails: ['masters-requirements-update@edx.opsgenie.net'],
    alwaysNotify: true
]

Map superCSV = [
    org: 'edx',
    repoName: 'super-csv',
    targetBranch: "master",
    pythonVersion: '3.5',
    cronValue: cronOffHoursBusinessWeekdayTwiceMonthly,
    githubUserReviewers: [],
    githubTeamReviewers: [],  // Reviewer mention unnecessary due to Master's OpsGenie alert.
    emails: ['masters-requirements-update@edx.opsgenie.net'],
    alwaysNotify: true
]

Map testengCI = [
    org: 'edx',
    repoName: 'testeng-ci',
    targetBranch: "master",
    pythonVersion: '2.7',
    cronValue: cronOffHoursBusinessWeekday,
    githubUserReviewers: [],
    githubTeamReviewers: ['devops'],
    emails: ['devops@edx.org'],
    alwaysNotify: false
]

Map videoEncodeManager = [
    org: 'edx',
    repoName: 'video-encode-manager',
    targetBranch: "master",
    pythonVersion: '3.6',
    cronValue: cronOffHoursBusinessWeekday,
    githubUserReviewers: [],
    githubTeamReviewers: ['vem-devel'],
    emails: ['azarembok@edx.org', 'dsheraz@edx.org', 'zamir@edx.org'],
    alwaysNotify: false
]

Map xblock = [
    org: 'edx',
    repoName: 'XBlock',
    targetBranch: "master",
    pythonVersion: '3.5',
    cronValue: cronOffHoursBusinessWeekdayLahore,
    githubUserReviewers: ['cpennington'],
    githubTeamReviewers: ['arbi-bom'],
    emails: ['arbi-bom@edx.org'],
    alwaysNotify: false
]

Map xblockUtils = [
    org: 'edx',
    repoName: 'xblock-utils',
    targetBranch: "master",
    pythonVersion: '3.5',
    cronValue: cronOffHoursBusinessWeekdayLahore,
    githubUserReviewers: [],
    githubTeamReviewers: ['arbi-bom'],
    emails: ['arbi-bom@edx.org'],
    alwaysNotify: false
]

Map xqueue = [
    org: 'edx',
    repoName: 'xqueue',
    targetBranch: "master",
    pythonVersion: '3.5',
    cronValue: cronOffHoursBusinessWeekday,
    githubUserReviewers: [],
    githubTeamReviewers: ['sustaining-team'],
    emails: ['sustaining-escalations@edx.org'],
    alwaysNotify: false
]

Map xssUtils = [
    org: 'edx',
    repoName: 'xss-utils',
    targetBranch: "master",
    pythonVersion: '3.5',
    cronValue: cronOffHoursBusinessWeekdayLahore,
    githubUserReviewers: [],
    githubTeamReviewers: ['arbi-bom'],
    emails: ['arbi-bom@edx.org'],
    alwaysNotify: false
]

List jobConfigs = [
    apiDocTools,
    authBackends,
    bokchoy,
    cc2olx,
    coachingPlugin,
    completion,
    configuration,
    cookiecutters,
    courseDiscovery,
    credentialsRepo,
    demographics,
    devstack,
    djangoConfigModels,
    djangoLangPrefMiddleware,
    djangoUserTasks,
    ecommerce,
    edxAce,
    edxAnalyticsDashboard,
    edxAnalyticsDataApi,
    edxBulkGrades,
    edxCeleryutils,
    edxDjangoUtils,
    edxDrfExtensions,
    edxE2eTests,
    edxEnterprise,
    edxGomatic,
    edxNotesApi,
    edxOra2,
    edxOrganizations,
    edxPlatform,
    edxProctoring,
    edxRbac,
    edxRepoHealth,
    edxRestApiClient,
    edxSphinxTheme,
    edxToggles,
    edxVal,
    notifier,
    opaqueKeys,
    openEdxStats,
    portalDesigner,
    pytestRepoHealth,
    registrar,
    staffGradedXBlock,
    superCSV,
    testengCI,
    videoEncodeManager,
    xblock,
    xblockUtils,
    xqueue,
    xssUtils,
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
            PR_USER_REVIEWERS: jobConfig.githubUserReviewers.join(','),
            PR_TEAM_REVIEWERS: jobConfig.githubTeamReviewers.join(',')
        )
        multiscm {
            git {
                remote {
                    credentials('jenkins-worker')
                    url("git@github.com:edx/${jobConfig.repoName}.git")
                }
                branch("${jobConfig.targetBranch}")
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
            jobConfig.alwaysNotify ? extendedEmail {
                triggers {
                    always {
                        recipientList(jobConfig.emails.join(' '))
                        content('''\
                        If the job succeeded, grab the PR link and merge it.
                        If the job failed, we need to fix that.

                        $BUILD_URL

                        $BUILD_LOG
                        '''.stripIndent())
                    }
                }
            } : mailer(jobConfig.emails.join(' '))
        }
    }

}

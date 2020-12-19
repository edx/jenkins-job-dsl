package testeng

// This script generates multiple jobs to run make upgrade across different repos
// Map exampleConfig = [
//     org: Github organization,
//     repoName: Github repository,
//     defaultBranch: the default branch of the repository. Ideally, this should be 'main'
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
// This schedule is split into even and odd options which can be used to prevent early and mid-month dry spells
def cronOffHoursBusinessWeekdayTwiceMonthlyEven = 'H H(4-11) 8-14,22-28 * H(1-5)'
// same but for Lahore-appropriate times
def cronOffHoursBusinessWeekdayLahoreTwiceMonthlyEven = 'H H(0-5) 8-14,22-28 * H(1-5)'
def cronOffHoursBusinessWeekdayTwiceMonthlyOdd = 'H H(4-11) 1-7,15-21 * H(1-5)'
def cronOffHoursBusinessWeekdayLahoreTwiceMonthlyOdd = 'H H(0-5) 1-7,15-21 * H(1-5)'

// Cron value for daily, sometime during: midnight-7:59am ET, Monday-Friday (times are in UTC)
// See https://github.com/jenkinsci/jenkins/blob/master/core/src/main/resources/hudson/triggers/TimerTrigger/help-spec.jelly
def cronOffHoursBusinessDaily = 'H H(4-11) * * 1-5'
// Cron value for daily, sometime during: 5am-09:59am Pakistan Time, Monday-Friday (times are in UTC)
def cronOffHoursBusinessDailyLahore = 'H H(0-5) * * 1-5'

List jobConfigs = [
    [
        org: 'edx',
        repoName: 'api-doc-tools',
        defaultBranch: 'master',
        pythonVersion: '3.5',
        cronValue: cronOffHoursBusinessWeekday,
        githubUserReviewers: [],
        githubTeamReviewers: [],
        emails: ['ned@edx.org'],
    ],
    [
        org: 'edx',
        repoName: 'auth-backends',
        defaultBranch: 'master',
        pythonVersion: '3.5',
        cronValue: cronOffHoursBusinessWeekdayLahore,
        githubUserReviewers: [],
        githubTeamReviewers: ['arbi-bom'],
        emails: ['arbi-bom@edx.org'],
        alwaysNotify: false
    ],
    [
        org: 'edx',
        repoName: 'bok-choy',
        defaultBranch: 'master',
        pythonVersion: '3.5',
        cronValue: cronOffHoursBusinessWeekdayLahore,
        githubUserReviewers: [],
        githubTeamReviewers: ['arbi-bom'],
        emails: ['arbi-bom@edx.org'],
        alwaysNotify: false
    ],
    [
        org: 'edx',
        repoName: 'cc2olx',
        defaultBranch: 'master',
        pythonVersion: '3.5',
        cronValue: cronOffHoursBusinessWeekdayTwiceMonthlyEven,
        githubUserReviewers: [],
        githubTeamReviewers: ['masters-devs-cosmonauts'],
        emails: ['masters-requirements-update@edx.opsgenie.net'],
    ],
    [
        org: 'edx',
        repoName: 'platform-plugin-coaching',
        defaultBranch: 'master',
        pythonVersion: '3.8',
        cronValue: cronOffHoursBusinessWeekday,
        githubUserReviewers: [],
        githubTeamReviewers: ['edx-aperture'],
        emails: ['aperture@edx.opsgenie.net'],
    ],
    [
        org: 'edx',
        repoName: 'configuration',
        defaultBranch: 'master',
        pythonVersion: '3.5',
        cronValue: cronOffHoursBusinessWeekday,
        githubUserReviewers: ['fredsmith'],
        githubTeamReviewers: ['devops'],
        emails: ['devops@edx.org'],
         alwaysNotify: false
    ],
    [
        org: 'edx',
        repoName: 'completion',
        defaultBranch: 'master',
        pythonVersion: '3.8',
        cronValue: cronOffHoursBusinessWeekday,
        githubUserReviewers: [],
        githubTeamReviewers: [],
        emails: ['engage-squad-eng@edx.org'],
        alwaysNotify: false
    ],
    [
        org: 'edx',
        repoName: 'edx-cookiecutters',
        defaultBranch: 'master',
        pythonVersion: '3.8',
        cronValue: cronOffHoursBusinessWeekdayLahore,
        githubUserReviewers: [],
        githubTeamReviewers: ['arbi-bom'],
        emails: ['arbi-bom@edx.org'],
        alwaysNotify: false
    ],
    [
        org: 'edx',
        repoName: 'course-discovery',
        defaultBranch: 'master',
        pythonVersion: '3.5',
        cronValue: cronOffHoursBusinessWeekday,
        githubUserReviewers: [],
        githubTeamReviewers: ['course-discovery-admins'],
        emails: ['mterry@edx.org'],
        alwaysNotify: false
    ],
    [
        org: 'edx',
        repoName: 'credentials',
        defaultBranch: 'master',
        pythonVersion: '3.8',
        cronValue: cronOffHoursBusinessWeekday,
        githubUserReviewers: [],
        githubTeamReviewers: ['edx-aperture'],
        emails: ['aperture@edx.opsgenie.net'],
    ],
    [
        org: 'edx',
        repoName: 'demographics',
        defaultBranch: 'master',
        pythonVersion: '3.8',
        cronValue: cronOffHoursBusinessWeekday,
        githubUserReviewers: [],
        githubTeamReviewers: ['edx-aperture'],
        emails: ['aperture@edx.opsgenie.net'],
    ],
    [
        org: 'edx',
        repoName: 'devstack',
        defaultBranch: 'master',
        pythonVersion: '3.5',
        cronValue: cronOffHoursBusinessWeekdayLahore,
        githubUserReviewers: [],
        githubTeamReviewers: ['arbi-bom'],
        emails: ['arbi-bom@edx.org'],
        alwaysNotify: false
    ],
    [
        org: 'edx',
        repoName: 'django-config-models',
        defaultBranch: 'master',
        pythonVersion: '3.5',
        cronValue: cronOffHoursBusinessWeekdayLahore,
        githubUserReviewers: [],
        githubTeamReviewers: ['arbi-bom'],
        emails: ['arbi-bom@edx.org'],
        alwaysNotify: false
    ],
    [
        org: 'edx',
        repoName: 'django-lang-pref-middleware',
        defaultBranch: 'master',
        pythonVersion: '3.5',
        cronValue: cronOffHoursBusinessWeekdayLahore,
        githubUserReviewers: [],
        githubTeamReviewers: ['arbi-bom'],
        emails: ['arbi-bom@edx.org'],
    ],
    [
        org: 'edx',
        repoName: 'django-user-tasks',
        defaultBranch: 'master',
        pythonVersion: '3.5',
        cronValue: cronOffHoursBusinessWeekdayLahore,
        githubUserReviewers: ['jmbowman'],
        githubTeamReviewers: ['arbi-bom'],
        emails: ['arbi-bom@edx.org'],
        alwaysNotify: false
    ],
    [
        org: 'edx',
        repoName: 'ecommerce',
        defaultBranch: 'master',
        pythonVersion: '3.8',
        cronValue: cronOffHoursBusinessWeekday,
        githubUserReviewers: [],
        githubTeamReviewers: ['ecommerce'],
        emails: ['revenue-tasks@edx.org'],
    ],
    [
        org: 'edx',
        repoName: 'edx-ace',
        defaultBranch: 'master',
        pythonVersion: '3.8',
        cronValue: cronOffHoursBusinessWeekdayLahore,
        githubUserReviewers: [],
        githubTeamReviewers: [],
        emails: ['engage-squad-eng@edx.org'],
        alwaysNotify: false
    ],
    [
        org: 'edx',
        repoName: 'edx-analytics-dashboard',
        defaultBranch: 'master',
        pythonVersion: '3.8',
        cronValue: cronOffHoursBusinessWeekday,
        githubUserReviewers: [],
        githubTeamReviewers: ['edx-data-engineering'],
        emails: ['data-engineering@edx.org'],
    ],
    [
        org: 'edx',
        repoName: 'edx-analytics-data-api',
        defaultBranch: 'master',
        pythonVersion: '3.8',
        cronValue: cronOffHoursBusinessWeekday,
        githubUserReviewers: [],
        githubTeamReviewers: ['edx-data-engineering'],
        emails: ['data-engineering@edx.org'],
    ],
    [
        org: 'edx',
        repoName: 'edx-bulk-grades',
        defaultBranch: 'master',
        pythonVersion: '3.5',
        cronValue: cronOffHoursBusinessWeekdayTwiceMonthlyEven,
        githubUserReviewers: [],
        githubTeamReviewers: [],  // Reviewer mention unnecessary due to Master's OpsGenie alert.
        emails: ['masters-requirements-update@edx.opsgenie.net'],
        alwaysNotify: true
    ],
    [
        org: 'edx',
        repoName: 'edx-celeryutils',
        defaultBranch: 'master',
        pythonVersion: '3.8',
        cronValue: cronOffHoursBusinessWeekdayLahore,
        githubUserReviewers: [],
        githubTeamReviewers: ['arbi-bom'],
        emails: ['arbi-bom@edx.org'],
        alwaysNotify: false
    ],
    [
        org: 'edx',
        repoName: 'edx-django-utils',
        defaultBranch: 'master',
        pythonVersion: '3.5',
        cronValue: cronOffHoursBusinessWeekday,
        githubUserReviewers: [],
        githubTeamReviewers: ['arbi-bom'],
        emails: ['arbi-bom@edx.org'],
    ],
    [
        org: 'edx',
        repoName: 'edx-drf-extensions',
        defaultBranch: 'master',
        pythonVersion: '3.5',
        cronValue: cronOffHoursBusinessWeekdayLahore,
        githubUserReviewers: [],
        githubTeamReviewers: ['arbi-bom'],
        emails: ['arbi-bom@edx.org'],
    ],
    [
        org: 'edx',
        repoName: 'edx-e2e-tests',
        defaultBranch: 'master',
        pythonVersion: '3.5',
        cronValue: cronOffHoursBusinessWeekdayLahore,
        githubUserReviewers: [],
        githubTeamReviewers: ['arbi-bom'],
        emails: ['arbi-bom@edx.org'],
        alwaysNotify: false
    ],
    [
        org: 'edx',
        repoName: 'edx-enterprise',
        defaultBranch: 'master',
        pythonVersion: '3.5',
        cronValue: cronOffHoursBusinessWeekday,
        githubUserReviewers: ['georgebabey'],
        githubTeamReviewers: ['business-enterprise-team'],
        emails: ['arbi-bom@edx.org'],
    ],
    [
        org: 'edx',
        repoName: 'edx-gomatic',
        defaultBranch: 'master',
        pythonVersion: '2.7',
        cronValue: cronOffHoursBusinessWeekday,
        githubUserReviewers: [],
        githubTeamReviewers: ['devops'],
        emails: ['devops@edx.org'],
    ],
    [
        org: 'edx',
        repoName: 'edx-notes-api',
        defaultBranch: 'master',
        pythonVersion: '3.8',
        cronValue: cronOffHoursBusinessWeekday,
        githubUserReviewers: [],
        githubTeamReviewers: ['devops'],
        emails: ['devops@edx.org'],
        alwaysNotify: false
    ],
    [
        org: 'edx',
        repoName: 'edx-ora2',
        defaultBranch: 'master',
        pythonVersion: '3.5',
        cronValue: cronOffHoursBusinessWeekdayTwiceMonthlyOdd,
        githubUserReviewers: [],
        githubTeamReviewers: [],  // Reviewer mention unnecessary due to Master's OpsGenie alert.
        emails: ['masters-requirements-update@edx.opsgenie.net'],
        alwaysNotify: true
    ],
    [
        org: 'edx',
        repoName: 'edx-organizations',
        defaultBranch: 'master',
        pythonVersion: '3.5',
        cronValue: cronOffHoursBusinessWeekdayLahore,
        githubUserReviewers: [],
        githubTeamReviewers: ['arbi-bom'],
        emails: ['arbi-bom@edx.org'],
        alwaysNotify: false
    ],
    [
        org: 'edx',
        repoName: 'edx-platform',
        defaultBranch: "master",
        pythonVersion: '3.8',
        cronValue: cronOffHoursBusinessDailyLahore,
        githubUserReviewers: [],
        githubTeamReviewers: ['arbi-bom'],
        emails: ['arbi-bom@edx.org'],
        alwaysNotify: false
    ],
    [
        org: 'edx',
        repoName: 'edx-proctoring',
        defaultBranch: 'master',
        pythonVersion: '3.5',
        cronValue: cronOffHoursBusinessWeekdayTwiceMonthlyEven,
        githubUserReviewers: [],
        githubTeamReviewers: [],  // Reviewer mention unnecessary due to Master's OpsGenie alert.
        emails: ['masters-requirements-update@edx.opsgenie.net'],
        alwaysNotify: true
    ],
    [
        org: 'edx',
        repoName: 'edx-rbac',
        defaultBranch: 'master',
        pythonVersion: '3.5',
        cronValue: cronOffHoursBusinessWeekdayLahore,
        githubUserReviewers: [],
        githubTeamReviewers: ['arbi-bom'],
        emails: ['arbi-bom@edx.org'],
        alwaysNotify: false
    ],
    [
        org: 'edx',
        repoName: 'edx-repo-health',
        defaultBranch: 'master',
        pythonVersion: '3.6',
        cronValue: cronOffHoursBusinessWeekdayLahore,
        githubUserReviewers: [],
        githubTeamReviewers: ['arbi-bom'],
        emails: ['arbi-bom@edx.org'],
        alwaysNotify: false
    ],
    [
        org: 'edx',
        repoName: 'edx-rest-api-client',
        defaultBranch: 'master',
        pythonVersion: '3.5',
        cronValue: cronOffHoursBusinessWeekdayLahore,
        githubUserReviewers: [''],
        githubTeamReviewers: ['arbi-bom'],
        emails: ['arbi-bom@edx.org'],
        alwaysNotify: false
    ],
    [
        org: 'edx',
        repoName: 'edx-sphinx-theme',
        defaultBranch: 'master',
        pythonVersion: '3.5',
        cronValue: cronOffHoursBusinessWeekdayLahore,
        githubUserReviewers: [''],
        githubTeamReviewers: ['arbi-bom'],
        emails: ['arbi-bom@edx.org'],
        alwaysNotify: false
    ],
    [
        org: 'edx',
        repoName: 'edx-toggles',
        defaultBranch: 'master',
        pythonVersion: '3.5',
        cronValue: cronOffHoursBusinessWeekdayLahore,
        githubUserReviewers: [],
        githubTeamReviewers: ['arbi-bom'],
        emails: ['arbi-bom@edx.org'],
        alwaysNotify: false
    ],
    [
        org: 'edx',
        repoName: 'edx-val',
        defaultBranch: 'master',
        pythonVersion: '3.5',
        cronValue: cronOffHoursBusinessWeekday,
        githubUserReviewers: [''],
        githubTeamReviewers: ['incident-management'],
        emails: ['incident-management@edx.org'],
        alwaysNotify: false
    ],
    [
        org: 'edx',
        repoName: 'edx-when',
        defaultBranch: 'master',
        pythonVersion: '3.8',
        cronValue: cronOffHoursBusinessWeekday,
        githubUserReviewers: [],
        githubTeamReviewers: [],
        emails: ['engage-squad-eng@edx.org'],
        alwaysNotify: false
    ],
    [
        org: 'edx',
        repoName: 'event-routing-backends',
        defaultBranch: 'master',
        pythonVersion: '3.8',
        cronValue: cronOffHoursBusinessWeekdayLahore,
        githubUserReviewers: [],
        githubTeamReviewers: ['arbi-bom'],
        emails: ['arbi-bom@edx.org'],
        alwaysNotify: false
    ],
    [
        org: 'edx',
        repoName: 'event-tracking',
        defaultBranch: 'master',
        // This repo had issues with the newest version of celery that gets pulled in when I build requirements in python
        // 3.8 so I'm leaving it at 3.5 for now so that arbi-bom can go through the upgrade with this when they are doing
        // this work for the other repos.
        pythonVersion: '3.5',
        cronValue: cronOffHoursBusinessWeekdayLahore,
        githubUserReviewers: [],
        githubTeamReviewers: ['arbi-bom'],
        emails: ['arbi-bom@edx.org'],
        alwaysNotify: false
    ],
    [
        org: 'edx',
        repoName: 'opaque-keys',
        defaultBranch: 'master',
        pythonVersion: '3.5',
        cronValue: cronOffHoursBusinessWeekdayLahore,
        githubUserReviewers: ['cpennington'],
        githubTeamReviewers: ['arbi-bom'],
        emails: ['arbi-bom@edx.org'],
        alwaysNotify: false
    ],
    [
        org: 'edx',
        repoName: 'openedxstats',
        defaultBranch: 'master',
        pythonVersion: '3.5',
        cronValue: cronOffHoursBusinessWeekday,
        githubUserReviewers: [],
        githubTeamReviewers: [],
        emails: ['ned@edx.org'],
        alwaysNotify: false
    ],
    [
        org: 'edx',
        repoName: 'portal-designer',
        defaultBranch: 'master',
        pythonVersion: '3.8',
        cronValue: cronOffHoursBusinessWeekdayTwiceMonthlyOdd,
        githubUserReviewers: [],
        githubTeamReviewers: [],  // Reviewer mention unnecessary due to Master's OpsGenie alert.
        emails: ['masters-requirements-update@edx.opsgenie.net'],
        alwaysNotify: true
    ],
    [
        org: 'edx',
        repoName: 'pytest-repo-health',
        defaultBranch: 'master',
        pythonVersion: '3.6',
        cronValue: cronOffHoursBusinessWeekdayLahore,
        githubUserReviewers: [],
        githubTeamReviewers: ['arbi-bom'],
        emails: ['arbi-bom@edx.org'],
        alwaysNotify: false
    ],
    [
        org: 'edx',
        repoName: 'registrar',
        defaultBranch: 'master',
        pythonVersion: '3.5',
        cronValue: cronOffHoursBusinessWeekdayTwiceMonthlyEven,
        githubUserReviewers: [],
        githubTeamReviewers: [],  // Reviewer mention unnecessary due to Master's OpsGenie alert.
        emails: ['masters-requirements-update@edx.opsgenie.net'],
        alwaysNotify: true
    ],
    [
        org: 'edx',
        repoName: 'repo-health-data',
        defaultBranch: 'master',
        pythonVersion: '3.8',
        cronValue: cronOffHoursBusinessWeekdayTwiceMonthlyEven,
        githubUserReviewers: [],
        githubTeamReviewers: ['arbi-bom'],  // Reviewer mention unnecessary due to Master's OpsGenie alert.
        emails: ['arbi-bom@edx.org'],
        alwaysNotify: false
    ],
    [
        org: 'edx',
        repoName: 'staff_graded-xblock',
        defaultBranch: 'master',
        pythonVersion: '3.5',
        cronValue: cronOffHoursBusinessWeekdayTwiceMonthlyOdd,
        githubUserReviewers: [],
        githubTeamReviewers: [],  // Reviewer mention unnecessary due to Master's OpsGenie alert.
        emails: ['masters-requirements-update@edx.opsgenie.net'],
        alwaysNotify: true
    ],
    [
        org: 'edx',
        repoName: 'super-csv',
        defaultBranch: 'master',
        pythonVersion: '3.5',
        cronValue: cronOffHoursBusinessWeekdayTwiceMonthlyEven,
        githubUserReviewers: [],
        githubTeamReviewers: [],  // Reviewer mention unnecessary due to Master's OpsGenie alert.
        emails: ['masters-requirements-update@edx.opsgenie.net'],
        alwaysNotify: true
    ],
    [
        org: 'edx',
        repoName: 'testeng-ci',
        defaultBranch: 'master',
        pythonVersion: '3.5',
        cronValue: cronOffHoursBusinessWeekday,
        githubUserReviewers: [],
        githubTeamReviewers: ['devops'],
        emails: ['devops@edx.org'],
        alwaysNotify: false
    ],
    [
        org: 'edx',
        repoName: 'video-encode-manager',
        defaultBranch: 'master',
        pythonVersion: '3.6',
        cronValue: cronOffHoursBusinessWeekday,
        githubUserReviewers: [],
        githubTeamReviewers: ['incident-management'],
        emails: ['incident-management@edx.org'],
        alwaysNotify: false
    ],
    [
        org: 'edx',
        repoName: 'XBlock',
        defaultBranch: 'master',
        pythonVersion: '3.5',
        cronValue: cronOffHoursBusinessWeekdayLahore,
        githubUserReviewers: ['cpennington'],
        githubTeamReviewers: ['arbi-bom'],
        emails: ['arbi-bom@edx.org'],
        alwaysNotify: false
    ],
    [
        org: 'edx',
        repoName: 'xblock-utils',
        defaultBranch: 'master',
        pythonVersion: '3.5',
        cronValue: cronOffHoursBusinessWeekdayLahore,
        githubUserReviewers: [],
        githubTeamReviewers: ['arbi-bom'],
        emails: ['arbi-bom@edx.org'],
        alwaysNotify: false
    ],
    [
        org: 'edx',
        repoName: 'xqueue',
        defaultBranch: 'master',
        pythonVersion: '3.5',
        cronValue: cronOffHoursBusinessWeekday,
        githubUserReviewers: [],
        githubTeamReviewers: ['arbi-bom'],
        emails: ['arbi-bom@edx.org'],
        alwaysNotify: false
    ],
    [
        org: 'edx',
        repoName: 'xss-utils',
        defaultBranch: 'master',
        pythonVersion: '3.5',
        cronValue: cronOffHoursBusinessWeekdayLahore,
        githubUserReviewers: [],
        githubTeamReviewers: ['arbi-bom'],
        emails: ['arbi-bom@edx.org'],
        alwaysNotify: false
    ],
]

seenRepoNames = []

/* Iterate over the job configurations */
jobConfigs.each { jobConfig ->

    // Null out and disable the version of these jobs that had the org
    // in the name. After these are deleted, this block can be removed.
    job("${jobConfig.org}-${jobConfig.repoName}-upgrade-python-requirements") {
        disabled()
    }

    if (jobConfig.repoName in seenRepoNames) {
        throw new IllegalArgumentException("Repo ${jobConfig.repoName} specified twice in upgrades list")
    }
    seenRepoNames.add(jobConfig.repoName)

    job("${jobConfig.repoName}-upgrade-python-requirements") {

        parameters {
            stringParam('TARGET_BRANCH', jobConfig.defaultBranch, 'Target branch to run make upgrade in')
        }

        logRotator {
            daysToKeep(14)
        }
        concurrentBuild(false)
        label('jenkins-worker')

        environmentVariables(
            PYTHON_VERSION: jobConfig.pythonVersion,
            TARGET_BRANCH: '$TARGET_BRANCH',
            PR_USER_REVIEWERS: jobConfig.githubUserReviewers.join(','),
            PR_TEAM_REVIEWERS: jobConfig.githubTeamReviewers.join(',')
        )
        multiscm {
            git {
                remote {
                    credentials('jenkins-worker')
                    url("git@github.com:${jobConfig.org}/${jobConfig.repoName}.git")
                }
                branch('$TARGET_BRANCH')
                extensions {
                    cleanBeforeCheckout()
                    relativeTargetDirectory('repo_to_upgrade')
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

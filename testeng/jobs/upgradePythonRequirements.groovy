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

// Cron value for daily, sometime during: midnight-7:59am ET, Monday-Friday (times are in UTC)
// See https://github.com/jenkinsci/jenkins/blob/master/core/src/main/resources/hudson/triggers/TimerTrigger/help-spec.jelly
def cronOffHoursBusinessDaily = 'H H(4-11) * * 1-5'
// Cron value for daily, sometime during: 5am-09:59am Pakistan Time, Monday-Friday (times are in UTC)
def cronOffHoursBusinessDailyLahore = 'H H(0-5) * * 1-5'

List jobConfigs = [
    [
        org: 'edx',
        repoName: 'configuration',
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
        repoName: 'edx-enterprise',
        defaultBranch: 'master',
        pythonVersion: '3.8',
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
]

seenRepoNames = []

/* Iterate over the job configurations */
jobConfigs.each { jobConfig ->

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

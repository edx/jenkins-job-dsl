// map from IDA to its corresponding edx repository; "" if no corresponding repository
final Map APPS_TO_REPO = [analytics_api: 'edx-analytics-data-api', credentials: 'credentials' , discovery: 'course-discovery', ecommerce: 'ecommerce', edxapp: 'edx-platform',
forum: 'cs_comments_service', harstorage: 'harstorage', insights: 'edx-analytics-dashboard', jenkins_analytics: '', nginx: '', rabbitmq: '', xqueue: 'xqueue',
xqwatcher: 'xqueue-watcher']

// map from IDA to its main branch
final Map APPS_TO_BRANCH = [analytics_api: 'master', credentials: 'master', discovery: 'master', ecommerce: 'master', edxapp: 'release', forum: 'master', harstorage: 'master',
insights: 'master', jenkins_analytics: '', nginx: '', rabbitmq: '', xqueue: 'master', xqwatcher: 'master']

final String CONFIGURATION_REPO_URL = 'https://github.com/edx/configuration.git'
final String CONFIGURATION_BRANCH = 'master'

final String EDX_REPO_ROOT = 'https://github.com/edx/'

// TODO: the Jenkins seed job should create credentials and use a user-entered ID (instead of UUID),
// so credentials can be referred to more easily from within the job.
// ID of credentials set up in Jenkins to log into DockerHub
final String JENKINS_CREDENTIALS_ID = '595676cc-61ce-41d1-b3c2-ee8578540650'

// create jobs in a separate folder in Jenkins
folder('applications-autobuilds')

def job = job('main') {
    // for each application
    APPS_TO_REPO.each { app_name, app_repo ->
        job('applications-autobuilds/' + app_name) {

            // add credentials to log in to DockerHub; ID refers to credential ID as set up in Jenkins
            wrappers {
                    credentialsBinding {
                        usernamePassword('USERNAME', 'PASSWORD', JENKINS_CREDENTIALS_ID)
                    }
            }
            multiscm {
                // check out edx/configuration repository from GitHub; necessary to be able to run dependency analyzer
                git {
                    remote {
                        url(CONFIGURATION_REPO_URL)
                    }
                    branch(CONFIGURATION_BRANCH)
                    relativeTargetDir('configuration')
                    // ignore notifications on commits to branch; this job will be triggered by
                    // configuration-watcher job
                    ignoreNotifyCommit()
                    // increase clone timeout to 3 hours
                    cloneTimeout(minutes = 360)

                }
                // if the IDA has a corresponding repository in the edx organization, checkout that repository
                if (app_repo) {
                    git {
                        remote {
                            url(EDX_REPO_ROOT + app_repo + '.git')
                        }
                        branch(APPS_TO_BRANCH.get(app_name))
                        relativeTargetDir('edx')
                        // increase clone timeout to 3 hours
                        cloneTimeout(minutes = 360)
                    }
                }
            }

            // polls configuration repository for changes every 10 minutes
            triggers {
                scm('H/10 * * * *')
            }

            steps {
                // inject name of the IDA as an environment variable
                environmentVariables {
                    env('APP_NAME', app_name)
                }

                // run the build-push-app shell script in a virtual environment called venv
                virtualenv {
                    name('venv')
                    nature('shell')
                    command readFileFromWorkspace('sample/resources/build-push-app.sh')
                }
            }
        }
    }
}

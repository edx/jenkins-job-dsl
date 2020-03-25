package testeng

import static org.edx.jenkins.dsl.JenkinsPublicConstants.GENERAL_SLACK_STATUS
import static org.edx.jenkins.dsl.JenkinsPublicConstants.JENKINS_PUBLIC_TEAM_SECURITY
import static org.edx.jenkins.dsl.JenkinsPublicConstants.JENKINS_PUBLIC_LOG_ROTATOR

job('build-packer-ami') {

    description('Create an AMI on aws based on json from the ' +
                'util/packer folder in the configuration repo.')

    // Special security scheme for members of a team
    authorization JENKINS_PUBLIC_TEAM_SECURITY.call(['edx*testeng'])

    parameters {
        stringParam('REMOTE_BRANCH', 'master',
                    'Branch of the configuration repo to use.')
        choiceParam('PACKER_JSON',
                    [ 'jenkins_worker.json',
                      'webpagetest.json',
                      'jenkins_worker_simple.json',
                      'jenkins_worker_android.json',
                      'jenkins_worker_codejail.json'
                    ],
                    'Json file (in util/packer) specifying how to build ' +
                    'the new AMI.')
        stringParam('PLATFORM_VERSION', 'master',
                    'the version of the edx-platform to run the smoke ' +
                    'tests against')
        choiceParam('DELETE_OR_KEEP', ['delete', 'keep'],
                    'What should we do with the AMI if it is ' +
                    'successfully built? (Hint: delete means you are ' +
                    'just testing the process.)')
        stringParam('JENKINS_WORKER_AMI', '',
                    'Override Base ami on which to run the Packer script')
    }

    logRotator JENKINS_PUBLIC_LOG_ROTATOR()
    concurrentBuild(true)
    label('coverage-worker')

    scm {
        git {
            remote {
                url('https://github.com/edx/configuration')
            }
            branch('\${REMOTE_BRANCH}')
            browser()
        }
    }

    triggers {
        cron('@daily')
    }

    wrappers {
        timeout {
            absolute(180)
            abortBuild()
        }
        timestamps()
        colorizeOutput('xterm')
        buildName('#${BUILD_NUMBER} ${ENV,var="BUILD_USER_ID"}')
        credentialsBinding {
            string('GITHUB_TOKEN', 'GITHUB_STATUS_OAUTH_TOKEN')
            string('JENKINS_WORKER_KEY_URL', 'JENKINS_WORKER_KEY_URL')
            string('NEWRELIC_INFRASTRUCTURE_LICENSE_KEY', 'NEWRELIC_INFRASTRUCTURE_LICENSE_KEY')
            string('WEBPAGETEST_BASE_AMI', 'PACKER_WEBPAGETEST_BASE_AMI')
            string('AWS_SECURITY_GROUP', 'PACKER_AWS_SECURITY_GROUP')

            amazonWebServicesCredentialsBinding {
                accessKeyVariable("AWS_ACCESS_KEY_ID")
                secretKeyVariable("AWS_SECRET_ACCESS_KEY")
                credentialsId("JENKINS_EC2_CREDENTIALS")
            }
        }
    }

    steps {
        shell(readFileFromWorkspace('testeng/resources/build-packer-ami.sh'))
    }

    publishers {
        // alert team of failures via slack & email
        configure GENERAL_SLACK_STATUS()
        mailer('testeng@edx.org')
    }

}

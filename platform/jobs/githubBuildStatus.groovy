package devops

import static org.edx.jenkins.dsl.DevopsConstants.common_wrappers
import static org.edx.jenkins.dsl.JenkinsPublicConstants.JENKINS_PUBLIC_LOG_ROTATOR

jobStringParameters = [
    [
        name: 'GIT_SHA',
        description: 'SHA for the git commit (must be the full 40 characters)',
        default: ''
    ],
    [
        name: 'GITHUB_ORG',
        description: 'Organization that owns the repo',
        default: ''
    ],
    [
        name: 'GITHUB_REPO',
        description: 'Name of the GitHub repo (e.g., for github.com/raccoongang/edx-platform, the value is "edx-platform"',
        default: ''

    ],
    [
        name: 'TARGET_URL',
        description: 'Link to build (must be HTTPS)',
        default: ''
    ],
    [
        name: 'DESCRIPTION',
        description: 'Short description of the build status as it appears on GitHub (e.g., "bokchoy -- Build passed")',
        default: ''
    ],
    [
        name: 'CONTEXT',
        description: 'A string label to indicate what service is providing this status (e.g. "jenkins/bokchoy")',
        default: ''
    ],
    [
        name: 'CREATE_DEPLOYMENT',
        description: 'When true: Also create a deployment event on GitHub for this commit.',
        default: 'false'
    ]
]

job('github-build-status') {

    wrappers common_wrappers
    wrappers {
        timeout {
            absolute(10)
        }
        timestamps()
        colorizeOutput()
        buildName('#${BUILD_NUMBER}')
        credentialsBinding {
            string('GITHUB_OAUTH_TOKEN', 'GITHUB_STATUS_OAUTH_TOKEN')
        }
    }
    logRotator JENKINS_PUBLIC_LOG_ROTATOR(1)
    parameters {
        jobStringParameters.each { param ->
                stringParam(param.name, param.default, param.description)
        }
        choiceParam(
                'BUILD_STATUS',
                ['pending', 'success', 'error', 'failure'],
                'Status of the build.'
            )
    }
    concurrentBuild()
    label('github-status-worker')
    steps {
        shell(readFileFromWorkspace('platform/resources/github_status_shell.sh'))
    }
}

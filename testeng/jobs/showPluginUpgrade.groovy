package testeng

import static org.edx.jenkins.dsl.JenkinsPublicConstants.JENKINS_PUBLIC_TEAM_SECURITY
import static org.edx.jenkins.dsl.JenkinsPublicConstants.JENKINS_PUBLIC_LOG_ROTATOR

job('show-jenkins-plugin-updates') {

    description('compare the plugin installations between two branches of the configuration repo')

    parameters {
        stringParam('BASE_BRANCH', 'master',
                    'Base branch of the configuration repo. This is used for comparing your changes to the existing state of Jenkins')
        stringParam('TARGET_BRANCH', 'master',
                    'Target branch of the configuration repo. This should contain your changes to the set of desired plugins')
        stringParam('PLUGIN_CONFIG_FILE', 'playbooks/roles/jenkins_build/defaults/main.yml',
                    'Path (relative to edx/configuration) to the file containig the list of plugins you are updating')
        stringParam('PLUGIN_CONFIG_KEY', 'build_jenkins_plugins_list',
                    'Key to the list of plugins within the PLUGIN_CONFIG file')
    }

    logRotator JENKINS_PUBLIC_LOG_ROTATOR()
    label('coverage-worker')

    scm {
        git {
            remote {
                url('https://github.com/edx/configuration')
            }
            branch('\${BASE_BRANCH}')
            browser()
            extensions {
                relativeTargetDirectory('configuration-base')
            }
        }
        git {
            remote {
                url('https://github.com/edx/configuration')
            }
            branch('\${TARGET_BRANCH}')
            browser()
            extensions {
                relativeTargetDirectory('configuration-target')
            }
        }
        git {
            remote {
                url('https://github.com/edx/jenkins-configuration')
            }
            branch('master')
            browser()
        }
    }

    wrappers {
        timeout {
            absolute(10)
            abortBuild()
        }
        timestamps()
        colorizeOutput('xterm')
    }

    steps {
        shell(readFileFromWorkspace('testeng/resources/show-jenkins-plugin-updates.sh'))
    }

}

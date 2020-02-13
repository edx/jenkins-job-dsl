package testeng

import static org.edx.jenkins.dsl.JenkinsPublicConstants.JENKINS_PUBLIC_LOG_ROTATOR
import static org.edx.jenkins.dsl.JenkinsPublicConstants.GENERAL_SLACK_STATUS

job('plan-plugin-upgrade-path') {


    logRotator JENKINS_PUBLIC_LOG_ROTATOR()
    label('jenkins-worker')
    concurrentBuild(false)

    parameters {
        stringParam(
            'JENKINS_CONFIG_BRANCH', 'master', '.'
        )
        stringParam(
            'BASE_CONFIG_BRANCH', 'master', '.'
        )
        stringParam(
            'BASE_CONFIG_PLUGIN_FILE',
            'base-configuration/playbooks/roles/jenkins_build/defaults/main.yml',
            '.'
        )
        stringParam(
            'BASE_CONFIG_PLUGIN_KEY', 'build_jenkins_plugins_list', '.'
        )
        stringParam(
            'TARGET_CONFIG_BRANCH', 'master', '.'
        )
        stringParam(
            'TARGET_CONFIG_PLUGIN_FILE',
            'base-configuration/playbooks/roles/jenkins_build/defaults/main.yml',
            '.'
        )
        stringParam(
            'TARGET_CONFIG_PLUGIN_KEY', 'build_jenkins_plugins_list', '.'
        )
    }

    multiscm {
        git {
            remote {
                url('https://github.com/edx/jenkins-configuration.git')
            }
            branch("${JENKINS_CONFIG_BRANCH}")
            extensions {
                relativeTargetDirectory('jenkins-configuration')
            }
        }
        git {
            remote {
                url('https://github.com/edx/configuration.git')
            }
            branch("${BASE_CONFIG_BRANCH}")
            extensions {
                relativeTargetDirectory('base-configuration')
            }
        }
        git {
            remote {
                url('https://github.com/edx/configuration.git')
            }
            branch("${TARGET_CONFIG_BRANCH}")
            extensions {
                relativeTargetDirectory('new-configuration')
            }
        }
    }

    wrappers {
        timestamps()
    }


    steps {
        shell(readFileFromWorkspace('testeng/resources/compare-plugin-updates.sh'))
    }


}

package testeng

import static org.edx.jenkins.dsl.JenkinsPublicConstants.JENKINS_PUBLIC_LOG_ROTATOR
import static org.edx.jenkins.dsl.JenkinsPublicConstants.JENKINS_PUBLIC_HIPCHAT

job('scan-plugin-updates') {

    description(
        'Scan the plugins installed on this Jenkins instance for security related updates'
    )
    authorization {
        blocksInheritance(true)
        permissionAll('edx*testeng')
    }

    logRotator JENKINS_PUBLIC_LOG_ROTATOR()
    label('jenkins-worker')
    concurrentBuild(false)

    scm {
        git {
            remote {
                url('https://github.com/edx/jenkins-configuration.git')
            }
            branch('*/master')
            extensions {
                relativeTargetDirectory('jenkins-configuration')
            }
        }
    }

    wrappers {
        timestamps()
    }

    triggers {
        cron('H H * * *')
    }

    steps {
        groovyScriptFile('scripts/scanPluginUpgrades.groovy') {
            groovyInstallation('groovy')
        }
    }

    publishers {
        mailer('testeng@edx.org')
        hipChatNotifier JENKINS_PUBLIC_HIPCHAT('')
    }

}

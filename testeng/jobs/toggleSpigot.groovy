package testeng

import hudson.util.Secret
import org.yaml.snakeyaml.Yaml
import static org.edx.jenkins.dsl.JenkinsPublicConstants.JENKINS_PUBLIC_MASKED_PASSWORD
import static org.edx.jenkins.dsl.JenkinsPublicConstants.JENKINS_PUBLIC_LOG_ROTATOR

Map config = [:]
Binding bindings = getBinding()
config.putAll(bindings.getVariables())
PrintStream out = config['out']

try {
    out.println('Parsing secret YAML file')
    String constantsConfig = new File("${TOGGLE_SPIGOT_SECRET}").text
    Yaml yaml = new Yaml()
    secretMap = yaml.load(constantsConfig)
    out.println('Successfully parsed secret YAML file')
}
catch (any) {
    out.println('Jenkins DSL: Error parsing secret YAML file')
    out.println('Exiting with error code 1')
    return 1
}

/*
    sampleJobConfig:
        toolsTeam: [ member1, member2, ... ]
        region: us-east-1
        accessKeyId: 123abc
        secretAccessKey: 123abc
        email: email@address
*/

// Iterate over the job configurations
secretMap.each { jobConfigs ->
    Map jobConfig = jobConfigs.getValue()

    assert jobConfig.containsKey('toolsTeam')
    assert jobConfig.containsKey('region')
    assert jobConfig.containsKey('accessKeyId')
    assert jobConfig.containsKey('secretAccessKey')
    assert jobConfig.containsKey('email')

    job('toggle-spigot') {

        description('Update the state of the spigot.')

        // Enable project security to avoid exposing aws keys
        authorization {
            blocksInheritance(true)
            jobConfig['toolsTeam'].each { member ->
                permissionAll(member)
            }
        }

        parameters {
            choiceParam('SPIGOT_STATE', ['ON', 'OFF'],
                        'Whether the spigot should be ON or OFF')
        }

        logRotator JENKINS_PUBLIC_LOG_ROTATOR()
        label('master')
        concurrentBuild(false)

        scm {
            git {
                remote {
                    url('https://github.com/edx/testeng-ci.git')
                }
                branch('*/master')
                browser()
            }
        }

        // Put sensitive info into masked password slots
        configure { project ->
            project / buildWrappers << 'EnvInjectPasswordWrapper' {
                injectGlobalPasswords false
                maskPasswordParameters true
                passwordEntries {
                    EnvInjectPasswordEntry {
                        name 'AWS_ACCESS_KEY_ID'
                        value Secret.fromString(jobConfig['accessKeyId']).getEncryptedValue()
                    }
                    EnvInjectPasswordEntry {
                        name 'AWS_SECRET_ACCESS_KEY'
                        value Secret.fromString(jobConfig['secretAccessKey']).getEncryptedValue()
                    }
                }
            }
        }

        environmentVariables {
            env('AWS_DEFAULT_REGION', jobConfig['region'])
            env('PYTHON_VERSION', '3.8')
            groovy(readFileFromWorkspace('testeng/resources/toggle-spigot-message.groovy'))
        }

        wrappers {
            timestamps()
        }

        steps {
            shell(readFileFromWorkspace('testeng/resources/toggle-spigot.sh'))
        }

        publishers {
            mailer(jobConfig['email'])
            configure {
                it / publishers / 'jenkins.plugins.slack.SlackNotifier' {
                    botUser true
                    startNotification false
                    notifySuccess true
                    notifyAborted false
                    notifyNotBuilt false
                    notifyUnstable false
                    notifyRegression false
                    notifyFailure false
                    notifyBackToNormal false
                    notifyRepeatedFailure false
                    includeTestSummary false
                    includeFailedTests false
                    includeCustomMessage true
                    customMessage '@here The Spigot is now: $SPIGOT_STATE ($SPIGOT_MESSAGE)'
                    room 'sre-notifications'
                    matrixTriggerMode ONLY_CONFIGURATIONS
                    commitInfoChoice NONE
                }
            }
        }
    }
}

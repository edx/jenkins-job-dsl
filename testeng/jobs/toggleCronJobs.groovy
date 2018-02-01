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
        email: email@address
*/

// Iterate over the job configurations
secretMap.each { jobConfigs ->
    Map jobConfig = jobConfigs.getValue()

    assert jobConfig.containsKey('toolsTeam')
    assert jobConfig.containsKey('email')

    job('toggle-cron-jobs') {

        description('Enable/Disable all cron jobs on Jenkins.')

        // Enable project security to avoid exposing aws keys
        authorization {
            blocksInheritance(true)
            jobConfig['toolsTeam'].each { member ->
                permissionAll(member)
            }
        }

        parameters {
            choiceParam('CRON_STATE', ['ON', 'OFF'],
                        'Whether cron jobs should be ON or OFF')
        }

        logRotator JENKINS_PUBLIC_LOG_ROTATOR()
        label('master')
        concurrentBuild(false)

        wrappers {
            timestamps()
        }

        steps {
            systemGroovyCommand(readFileFromWorkspace('testeng/resources/toggle-cron-jobs.groovy'))
        }

        publishers {
            mailer(jobConfig['email'])
        }
    }
}

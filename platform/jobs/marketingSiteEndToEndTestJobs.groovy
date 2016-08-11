package devops

import hudson.util.Secret
import org.yaml.snakeyaml.Yaml
import static org.edx.jenkins.dsl.JenkinsPublicConstants.JENKINS_PUBLIC_WORKER

/* Sample Secret File 
marketingUrlRoot : marketing-url-root
*/

/* stdout logger */
Map config = [:]
Binding bindings = getBinding()
config.putAll(bindings.getVariables())
PrintStream out = config['out']

/* Map to hold the k:v pairs parsed from the secret file */
Map secretMap = [:]
Map marketingMap = [:]
try {
    out.println('Parsing secret YAML file')
    String sharedFileContents = new File("${EDX_SHARED_SECRET}").text
    String secretFileContents = new File("${EDX_MARKETING_END_TO_END_SECRET}").text
    Yaml yaml = new Yaml()
    secretMap = yaml.load(sharedFileContents)
    marketingMap = yaml.load(secretFileContents)
    out.println('Successfully parsed secret YAML file')
}
catch (any) {
    out.println('Jenkins DSL: Error parsing secret YAML file')
    out.println('Exiting with error code 1')
    return 1
}

/* Test secret contains all necessary keys for this job */
assert secretMap.containsKey('credential')
assert marketingMap.containsKey('marketingUrlRoot')

parameter = [
    name: 'MARKETING_URL_ROOT',
    description: 'URL root for the marketing deployment against which to run tests.',
    default: marketingMap['marketingUrlRoot'] ]

String jobName = 'marketing-end-to-end-tests'

job(jobName) {
    description('Job to execute marketing site end-to-end tests.')
    configure {
        it / 'properties' / 'hudson.model.ParametersDefinitionProperty' / parameterDefinitions << 'hudson.model.PasswordParameterDefinition' {
            name parameter.name
            defaultValue Secret.fromString(parameter.default.toString()).getEncryptedValue()
            description parameter.description
        }
    }
    label(JENKINS_PUBLIC_WORKER)
    scm {
        git {
            remote {
                github('edx/edx-mktg')
                credentials(secretMap['credential'])
            }
            branch('*/master')
        }
    }
    wrappers {
        maskPasswords() //can't see passwords in console
        timestamps()
        buildUserVars()
        colorizeOutput('xterm')
    }
    steps {
        virtualenv {
            name(jobName)
            nature('shell')
            command readFileFromWorkspace('platform/resources/end-to-end-test.sh')
        }
    }
    publishers {
        archiveArtifacts {
            pattern('acceptance_tests/*.xml') // xunit output
            pattern('*.log') // test logs
            pattern('*.png') // test screenshots
        }
        archiveJunit('acceptance_tests/*.xml')
    }
}

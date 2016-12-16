package devops

import hudson.util.Secret
import org.yaml.snakeyaml.Yaml
import static org.edx.jenkins.dsl.JenkinsPublicConstants.JENKINS_PUBLIC_WORKER

/* Sample Secret File
lmsUrlRoot : lms-url-root
programsUrlRoot : programs-url-root
studioUrlRoot : studio-url-root
allowDeleteAllProgram : true/false
programOrganization : program-organization
lmsAutoAuth : true/false
studioEmail : studio@email.com
studioPassword : studio-password
basicAuthUsername : basic-auth-username
basicAuthPassword : basic-auth-password
*/

/* stdout logger */
Map config = [:]
Binding bindings = getBinding()
config.putAll(bindings.getVariables())
PrintStream out = config['out']

/* Map to hold the k:v pairs parsed from the secret file */
Map secretMap = [:]
try {
    out.println('Parsing secret YAML file')
    String secretFileContents = new File("${EDX_PROGRAMS_END_TO_END_SECRET}").text
    Yaml yaml = new Yaml()
    secretMap = yaml.load(secretFileContents)
    out.println('Successfully parsed secret YAML file')
}
catch (any) {
    out.println('Jenkins DSL: Error parsing secret YAML file')
    out.println('Exiting with error code 1')
    return 1
}

/* Test secret contains all necessary keys for this job */
assert secretMap.containsKey('lmsUrlRoot')
assert secretMap.containsKey('programsUrlRoot')
assert secretMap.containsKey('studioUrlRoot')
assert secretMap.containsKey('allowDeleteAllProgram')
assert secretMap.containsKey('programOrganization')
assert secretMap.containsKey('lmsAutoAuth')
assert secretMap.containsKey('studioEmail')
assert secretMap.containsKey('studioPassword')
assert secretMap.containsKey('basicAuthUsername')
assert secretMap.containsKey('basicAuthPassword')

parameters = [
    [
        name: 'LMS_URL_ROOT',
        description: 'The URL of the LMS instance the test to run against',
        default: secretMap['lmsUrlRoot']
    ],
    [
        name: 'PROGRAMS_URL_ROOT',
        description: 'The URL of the program instance the test to run against',
        default: secretMap['programsUrlRoot']
    ],
    [
        name: 'STUDIO_URL_ROOT',
        description: 'The URL of Studio instance the test to run against',
        default: secretMap['studioUrlRoot']
    ],
    [
        name: 'ALLOW_DELETE_ALL_PROGRAM',
        description: 'Whether allow this test to set all the programs in the target environment to \"deleted\" status',
        default: secretMap['allowDeleteAllProgram']
    ],
    [
        name: 'PROGRAM_ORGANIZATION',
        description: 'The organization to connect the new program to during the test',
        default: secretMap['programOrganization']
    ],
    [
        name: 'LMS_AUTO_AUTH',
        description: 'Whether in the target environment, the auto_auth feature is enabled. ' +
                     '(auto_auth is a way to auto create test users)',
        default: secretMap['lmsAutoAuth']
    ],
    [
        name: 'STUDIO_EMAIL',
        description: 'The staff account email used to log into studio',
        default: secretMap['studioEmail']
    ],
    [
        name: 'STUDIO_PASSWORD',
        description: 'The staff account password used to log into studio',
        default: secretMap['studioPassword']
    ],
    [
        name: 'BASIC_AUTH_USERNAME',
        description: 'The username for basic auth for target environments',
        default: secretMap['basicAuthUsername']
    ],
    [
        name: 'BASIC_AUTH_PASSWORD',
        description: 'The password for basic auth for target environments',
        default: secretMap['basicAuthPassword']
    ]
]

String jobName = 'programs-end-to-end-tests'

job(jobName) {
    description('Job to execute programs IDA end-to-end tests. ' +
                'These tests require access to the program administration tool in Studio.')

    /* Allow only edx members to see this job */
    authorization {
        blocksInheritance(true)
        permissionAll('edx')
    }

    /* Add the parameters as password parametersi by configuring the XML, */
    /* Ensure that parameter's default values cannot be seen */
    parameters.each { param ->
        configure {
            it / 'properties' / 'hudson.model.ParametersDefinitionProperty' / parameterDefinitions << 'hudson.model.PasswordParameterDefinition' {
                name param.name
                defaultValue Secret.fromString(param.default.toString()).getEncryptedValue()
                description param.description
            }
        }
    }
    /* Run on jenkins-worker */
    label(JENKINS_PUBLIC_WORKER)
    scm {
        git {
            remote {
                url('https://github.com/edx/programs.git')
            }
            browser()
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

package testeng

import hudson.util.Secret
import org.yaml.snakeyaml.Yaml
import static org.edx.jenkins.dsl.JenkinsPublicConstants.GENERAL_SLACK_STATUS
import static org.edx.jenkins.dsl.JenkinsPublicConstants.JENKINS_PUBLIC_TEAM_SECURITY
import static org.edx.jenkins.dsl.JenkinsPublicConstants.JENKINS_PUBLIC_LOG_ROTATOR

Map config = [:]
Binding bindings = getBinding()
config.putAll(bindings.getVariables())
PrintStream out = config['out']

try {
    out.println('Parsing secret YAML file')
    /* Parse k:v pairs from the secret file referenced by secretFileVariable */
    String contents = new File("${BUILD_PACKER_AMI_SECRET}").text
    Yaml yaml = new Yaml()
    secretMap = yaml.load(contents)
    out.println('Successfully parsed secret YAML file')
}
catch (any) {
    out.println('Jenkins DSL: Error parsing secret YAML file')
    out.println('Exiting with error code 1')
    return 1
}

/*
    sampleJobConfig:
        awsAccessKeyId: 123abc
        awsSecretAccessKey: 123abc
        awsSecurityGroup: 123
        newRelicKey: 123abc
        webPageTestBaseAMI: ami-123
        toolsTeam: [ 'users1', 'users2' ]
        email : email@address
*/

/* Iterate over the job configurations */
secretMap.each { jobConfigs ->
    Map jobConfig = jobConfigs.getValue()

    assert jobConfig.containsKey('awsAccessKeyId')
    assert jobConfig.containsKey('awsSecretAccessKey')
    assert jobConfig.containsKey('awsSecurityGroup')
    assert jobConfig.containsKey('newRelicKey')
    assert jobConfig.containsKey('webPageTestBaseAMI')
    assert jobConfig.containsKey('toolsTeam')
    assert jobConfig.containsKey('email')
    assert jobConfig.containsKey('sshKeyURL')

    job('build-packer-ami') {

        description('Create an AMI on aws based on json from the ' +
                    'util/packer folder in the configuration repo.')

        // Special security scheme for members of a team
        authorization JENKINS_PUBLIC_TEAM_SECURITY.call(jobConfig['toolsTeam'])

        parameters {
            stringParam('REMOTE_BRANCH', 'master',
                        'Branch of the configuration repo to use.')
            choiceParam('PACKER_JSON',
                        [ 'jenkins_worker.json',
                          'webpagetest.json',
                          'jenkins_worker_simple.json',
                          'jenkins_worker_android.json',
                          'jenkins_worker_loadtest.json'
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
            stringParam('JENKINS_WORKER_AMI', 'ami-aa2ea6d0',
                        'Base ami on which to run the Packer script')
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

        // Run job every 2 hours
        triggers {
            cron('H H/2 * * *')
        }

        wrappers {
            timeout {
                absolute(180)
                abortBuild()
            }
            timestamps()
            colorizeOutput('xterm')
            buildName('#${BUILD_NUMBER} ${ENV,var="BUILD_USER_ID"}')
        }

        environmentVariables{
	    env('JENKINS_WORKER_KEY_URL', jobConfig['sshKeyURL'])
        }

        // Put sensitive info into masked password slots
        configure { project ->
            project / buildWrappers << 'EnvInjectPasswordWrapper' {
                injectGlobalPasswords false
                maskPasswordParameters true
                passwordEntries {
                    EnvInjectPasswordEntry {
                        name 'NEW_RELIC_KEY'
                        value Secret.fromString(jobConfig['newRelicKey']).getEncryptedValue()
                    }
                    EnvInjectPasswordEntry {
                        name 'AWS_ACCESS_KEY_ID'
                        value Secret.fromString(jobConfig['awsAccessKeyId']).getEncryptedValue()
                    }
                    EnvInjectPasswordEntry {
                        name 'AWS_SECRET_ACCESS_KEY'
                        value Secret.fromString(jobConfig['awsSecretAccessKey']).getEncryptedValue()
                    }
                    EnvInjectPasswordEntry {
                        name 'AWS_SECURITY_GROUP'
                        value Secret.fromString(jobConfig['awsSecurityGroup']).getEncryptedValue()
                    }
                    EnvInjectPasswordEntry {
                        name 'WEBPAGE_TEST_BASE_AMI'
                        value Secret.fromString(jobConfig['webPageTestBaseAMI']).getEncryptedValue()
                    }
                }
            }
        }

        steps {
            shell(readFileFromWorkspace('testeng/resources/build-packer-ami.sh'))
        }

        publishers {
            // alert team of failures via slack & email
            configure GENERAL_SLACK_STATUS()
            mailer(jobConfig['email'])
        }

    }
}

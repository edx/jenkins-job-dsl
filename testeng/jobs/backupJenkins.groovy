package testeng

import hudson.util.Secret
import org.yaml.snakeyaml.Yaml
import static org.edx.jenkins.dsl.JenkinsPublicConstants.GENERAL_SLACK_STATUS

Map config = [:]
Binding bindings = getBinding()
config.putAll(bindings.getVariables())
PrintStream out = config['out']

try {
    out.println('Parsing secret YAML file')
    /* Parse k:v pairs from the secret file referenced by secretFileVariable */
    String contents = new File("${BACKUP_JENKINS_SECRET}").text
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
Example secret YAML file used by this script
Config:
    volumeId : vol-123
    region : us-west.1
    accessKeyId : 123
    secretAccessKey: 123
    email : email@address.com
*/

/* Iterate over the job configurations */
secretMap.each { jobConfigs ->

    Map jobConfig = jobConfigs.getValue()

    assert jobConfig.containsKey('volumeId')
    assert jobConfig.containsKey('region')
    assert jobConfig.containsKey('accessKeyId')
    assert jobConfig.containsKey('secretAccessKey')
    assert jobConfig.containsKey('email')

    job("backup-build-jenkins") {

        // private job
        authorization {
            blocksInheritance(true)
            permissionAll('edx')
        }

        description('Create an EC2 snapshot of the build jenkins data volume')
        // Keep logs longer than normal jenkins jobs
        logRotator {
            numToKeep(50)
        }
        concurrentBuild(false)
        label('backup-runner')

        // Run the job daily at 1AM
        triggers {
            cron('0 1 * * *')
        }

        // This should take < 10 seconds in most cases, as it is only
        // kicking off the snapshot, not waiting for it to complete.
        // Giving it enough time for the case that it needs to install new requirements.
        wrappers {
            timeout {
                absolute(5)
                abortBuild()
            }
            timestamps()
            colorizeOutput('xterm')
        }

        // load env vars for executing awscli commands
        environmentVariables {
            env('AWS_DEFAULT_REGION', jobConfig['region'])
        }
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

        // Sync currently paged files to disk
        String script = 'sync\n'
        // This might seem overkill, but in case the pip requirements change, read them from
        // the requirements file in the workspace
        readFileFromWorkspace('testeng/resources/requirements.txt').split('\n').each { line ->
            script += "pip install --exists-action w ${line}\n"
        }
        script += "aws ec2 create-snapshot --volume-id ${jobConfig['volumeId']} " +
                  "--description 'Data volume snapshot from the backup-build-jenkins job'" +
                  "> \${WORKSPACE}/snapshot-out.log\n"
        script += "cat \${WORKSPACE}/snapshot-out.log"
        steps {
            virtualenv {
                clear()
                name('venv')
                pythonName('System-CPython-3.5')
                nature('shell')
                command(script)
            }
        }

        publishers {
            // archive the snapshot tool output
            archiveArtifacts {
                pattern('snapshot-out.log')
                allowEmpty(false)
            }
            // alert team of failures via slack & email
            configure GENERAL_SLACK_STATUS()


            mailer(jobConfig['email'])
            // fail the build if the snapshot command does not correctly trigger a snapshot
            // requires "textFinder plugin"
            textFinder('"State": "(pending|completed)"', 'snapshot-out.log', false, true, false)
        }
    }
}

package testeng

import org.yaml.snakeyaml.Yaml

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
    jenkinsInstance : test
    volumeId : vol-123
    region : us-west.1
*/

/* Iterate over the job configurations */
secretMap.each { jobConfigs ->

    Map jobConfig = jobConfigs.getValue()

    assert jobConfig.containsKey('jenkinsInstance')
    assert jobConfig.containsKey('volumeId')
    assert jobConfig.containsKey('region')

    job("backup-${jobConfig['jenkinsInstance']}-jenkins") {
        
        // private job
        authorization {
            blocksInheritance(true)
            permissionAll('edx')
        }

        description('A regularly run job for creating snapshots of build jenkins master ' +
                    ' and uploading them to Amazon s3')
        // Keep logs longer than normal jenkins jobs
        logRotator {
            numToKeep(50)
        }
        concurrentBuild(false)
        label("micro-worker")

        // Configure the Exclusive Execution plugin, to reduce the amount of things in memory during snapshotting
        configure { project ->
            project / buildWrappers << 'hudson.plugins.execution.exclusive.ExclusiveBuildWrapper' {
                skipWaitOnRunningJobs false
            }
        }

        wrappers {
            timeout {
                absolute(20)
                abortBuild()
            }
            timestamps()
            colorizeOutput('xterm')
        }

        environmentVariables {
            env('AWS_DEFAULT_REGION', jobConfig['region'])
        }
        
        // This might seem overkill, but in case the pip requirements change, read them from
        // the requirements file in the workspace
        String script = ""
        readFileFromWorkspace('testeng/resources/requirements.txt').split("\n").each { line ->
            script += "pip install ${line}; "
        }
        script += "aws ec2 create-snapshot --volume-id ${jobConfig['volumeId']} --description 'Automatic ${jobConfig['jenkinsInstance']} jenkins snapshot'"
        steps {
            virtualenv {
                clear()
                name('venv')
                nature('shell')
                command(script)
            }
        }
    }
    
}

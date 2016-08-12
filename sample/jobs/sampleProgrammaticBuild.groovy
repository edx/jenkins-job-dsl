final String CONFIGURATION_REPO_URL = 'https://github.com/edx/configuration.git'
final String CONFIGURATION_BRANCH = 'master'

final String EDX_REPO_ROOT = 'https://github.com/edx/'
final String EDX_REPO_BRANCH = 'release'

def job = job('configuration-watcher') {
    // check out edx/configuration repository from GitHub
    scm {
        git {
            remote {
                url(CONFIGURATION_REPO_URL)
                relativeTargetDir('configuration')
                branch(CONFIGURATION_BRANCH)
            }
        }
    }
    // polls configuration repository for changes every 4 minutes
    triggers {
        scm('H/10 * * * *')
    }

    // run the trigger-builds shell script in a virtual environment called venv
    steps {
        virtualenv {
            name('venv')
            nature('shell')
            command readFileFromWorkspace('sample/resources/trigger-builds.sh')
        }

        // inject environment variables defined in the temp_props file
        environmentVariables {
            propertiesFile('temp_props')
        }
        
        // trigger the jobs defined in the TO_BUILD environment variable; this is set via the trigger-builds script 
        // and injected into the environment from the temp_props file
        downstreamParameterized {
            trigger('${TO_BUILD}')
        }
    }
}

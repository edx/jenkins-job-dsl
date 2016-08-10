t_steps = '''#!/usr/bin/env bash -ex

cd edx-gomatic
make test_requirements
make test
'''

job('TestGomatic'){
    multiscm {
        git {
            remote {
                url("https://github.com/edx/edx-gomatic")
                branch("origin/feanil/update_deploy_output")
            }
            createTag(false)
            extensions {
                relativeTargetDirectory('edx-gomatic')
            }


        }
        git {
            remote {
                url("git@github.com:edx-ops/gomatic-secure.git")
                branch("origin/master")
              	credentials('TBD')

            }
            createTag(false)
            extensions {
                relativeTargetDirectory('gomatic-secure')
            }

        }
    }

    steps {
      virtualenv {
          command(test_steps)
      }
		
    }
}

package mobile

import org.yaml.snakeyaml.Yaml
import static org.edx.jenkins.dsl.JenkinsPublicConstants.JENKINS_PUBLIC_LOG_ROTATOR

/* stdout logger */
/* use this instead of println, because you can pass it into closures or other scripts. */
Map config = [:]
Binding bindings = getBinding()
config.putAll(bindings.getVariables())
PrintStream out = config['out']

out.println('Parsing secret YAML file')
try {
    /* Parse k:v pairs from the secret file referenced by secretFileVariable */
    String contents = new File("${BUILD_ANDROID_SECRET}").text
    Yaml yaml = new Yaml()
    secretMap = yaml.load(contents)
}
catch (any) {
    out.println('Jenkins DSL: Error parsing secret YAML file')
    out.println('Exiting with error code 1')
    return 1
}
out.println('Successfully parsed secret YAML file')

// Iterate over the configurations in BUILD_ANDROID_SECRET
secretMap.each { jobConfigs ->

    Map jobConfig = jobConfigs.getValue()

    // Test parsed secret contains the correct fields

    // Create a Jenkins job
    job(jobConfig['jobName']) {

        // This is a private job
        authorization {
            blocksInheritance(true)
            permissionAll('edx')
        }

        description('Compile, sign, test and publish the edX Android app to HockeyApp')

        // Clean up older builds/logs
        logRotator JENKINS_PUBLIC_LOG_ROTATOR()
        
        concurrentBuild(false)
        // Run this build on the Jenkins worker configured with the Android SDK
        label('android-worker')

        // Parameterize build with "HASH", the git hash used to pull source
        parameters {
            stringParam('HASH', 'refs/heads/master', 'Git hash of the project to build. Default = most recent hash of master.')
        }

        scm {
            git { 
                remote {
                    url(jobConfig['gitRepo'])
                    if (!jobConfig['public'].toBoolean()) {
                        credentials(jobConfig['gitCredential'])
                    }
                    refspec('+refs/heads/master:refs/remotes/origin/master')
                }
                branch('\${HASH}')
                browser()
                extensions {
                    cleanBeforeCheckout()
                    relativeTargetDirectory(jobConfig['appBaseDir'])
                }
            }
        }

        wrappers {
            timeout {
                absolute(20)
                abortBuild()
            }
            timestamps()
            colorizeOutput('xterm')
            // Recursively use ssh while initializing git submodules
            sshAgent(jobConfig['sshAgent'])
        }

        // Inject environment variables to make the shell script generic
        environmentVariables {
            env('APP_BASE_DIR', jobConfig['appBaseDir'])
            env('APP_BASE_NAME', jobConfig['appBaseName'])
        }

        // Run the scripts to build and rename the app
        steps {
            def buildScript = readFileFromWorkspace('mobileApp/resources/organize_android_app_build.sh')
            shell(buildScript)
        }

        publishers {
            // Archive the artifacts, in this case, the APK file:
            archiveArtifacts {
                allowEmpty(false)
                pattern('artifacts/\$APP_BASE_NAME.*')
            }
        }

    }
}

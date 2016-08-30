package mobile

import org.yaml.snakeyaml.Yaml
import static org.edx.jenkins.dsl.JenkinsPublicConstants.JENKINS_PUBLIC_LOG_ROTATOR

/*
Example secret YAML file used by this script
jobConfig:
    jobName: build-demo-android-app
    public: true
    release: false
    buildScript: build.sh
    gradleProperties: GRADLE_PROPERTIES
    keystore: APP_KEYSTORE
    keystoreFile: APP_KEYSTORE_FILE
    gitRepo: https://github.com/org/project.git
    gitCredential: git_user
    appBaseDir: android-app-project
    appBaseName: edx-demo-app
    generatedApkName: edx-demo-app.apk
    sshAgent: ssh_user
    hockeyAppApiToken: abc123
*/

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
    assert jobConfig.containsKey('jobName')
    assert jobConfig.containsKey('public')
    assert jobConfig.containsKey('release')
    assert jobConfig.containsKey('buildScript')
    assert jobConfig.containsKey('gradleProperties')
    assert jobConfig.containsKey('keystore')
    assert jobConfig.containsKey('keystoreFile')
    assert jobConfig.containsKey('gitRepo')
    assert jobConfig.containsKey('gitCredential')
    assert jobConfig.containsKey('appBaseDir')
    assert jobConfig.containsKey('appBaseName')
    assert jobConfig.containsKey('generatedApkName')
    assert jobConfig.containsKey('sshAgent')
    assert jobConfig.containsKey('hockeyAppApiToken')

    // Create a Jenkins job
    job(jobConfig['jobName']) {

        // This is a private job
        if (!jobConfig['public']) {
            authorization {
                blocksInheritance(true)
                permissionAll('edx')
            }
        }

        description('Compile, sign, test and publish the edX Android app to HockeyApp')

        // Clean up older builds/logs
        logRotator JENKINS_PUBLIC_LOG_ROTATOR()
        
        concurrentBuild(false)
        // Run this build on the Jenkins worker configured with the Android SDK
        label('android-worker')

        // Parameterize build with:
        //"HASH", the git hash used to pull the app source code
        //"RELEASE_NOTES", a string (multi-line) for annotating the builds published to hockeyApp
        parameters {
            stringParam('HASH', 'refs/heads/master', 'Git hash of the project to build. Default = most recent hash of master.')
            textParam('RELEASE_NOTES', 'Test Build', 'Plain text release notes. Add content here and it will be published to HockeyApp along with the app.')
        }

        scm {
            git { 
                remote {
                    url(jobConfig['gitRepo'])
                    if (!jobConfig['public'].toBoolean()) {
                        credentials(jobConfig['gitCredential'])
                    }
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

        // Inject environment variables to make the build script generic
        environmentVariables {
            env('ANDROID_HOME', '/opt/Android/Sdk')
            env('BUILD_SCRIPT', jobConfig['buildScript'])
            env('APP_BASE_DIR', jobConfig['appBaseDir'])
            env('APP_BASE_NAME', jobConfig['appBaseName'])
            env('GEN_APK', jobConfig['generatedApkName'])
            env('GRADLE_PROPERTIES', jobConfig['gradleProperties'])
            env('KEY_STORE', jobConfig['keystore'])
            env('KEY_STORE_FILE', jobConfig['keystoreFile'])
        }

        steps {
            // Run the scripts to build and rename the app
            def copyKeyScript = readFileFromWorkspace('mobileApp/resources/copy_keys.sh')
            def buildScript = readFileFromWorkspace('mobileApp/resources/organize_android_app_build.sh')
            def testSigningScript = readFileFromWorkspace('mobileApp/resources/check_release_build.sh')
            // Copy keystores into workspace for release builds
            if (jobConfig['release']) {
                shell(copyKeyScript)
            }
            shell(buildScript)
            // Verify that they are signed properly for release builds
            if (jobConfig['release']) {
                shell(testSigningScript)
            }
        }

        publishers {
            // Archive the artifacts, in this case, the APK file:
            archiveArtifacts {
                allowEmpty(false)
                pattern('artifacts/\$APP_BASE_NAME.*')
            }

            // Publish the application to HockeyApp
            configure { project ->
                project / publishers << 'hockeyapp.HockeyappRecorder' (schemaVersion: '2') {
                    applications {
                        'hockeyapp.HockeyappApplication' (plugin: 'hockeyapp@1.2.1', schemaVersion: '1') {
                            apiToken jobConfig['hockeyAppApiToken']
                            notifyTeam true
                            filePath 'artifacts/*.apk'
                            dowloadAllowed true
                            releaseNotesMethod (class: "net.hockeyapp.jenkins.releaseNotes.ManualReleaseNotes") {
                                releaseNotes "\${RELEASE_NOTES}"
                                isMarkDown false
                            }
                            uploadMethod (class: 'net.hockeyapp.jenkins.uploadMethod.AppCreation') {}
                        }
                    }
                }
            }
        }
    }
}

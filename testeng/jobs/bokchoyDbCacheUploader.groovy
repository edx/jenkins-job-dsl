package testeng

import hudson.util.Secret
import org.yaml.snakeyaml.Yaml
import static org.edx.jenkins.dsl.JenkinsPublicConstants.JENKINS_PUBLIC_MASKED_PASSWORD
import static org.edx.jenkins.dsl.JenkinsPublicConstants.JENKINS_PUBLIC_LOG_ROTATOR
import static org.edx.jenkins.dsl.JenkinsPublicConstants.GENERAL_SLACK_STATUS
import static org.edx.jenkins.dsl.JenkinsPublicConstants.JENKINS_PUBLIC_BASE_URL
import static org.edx.jenkins.dsl.JenkinsPublicConstants.JENKINS_PUBLIC_GITHUB_BASEURL

Map config = [:]
Binding bindings = getBinding()
config.putAll(bindings.getVariables())
PrintStream out = config['out']

try {
    out.println('Parsing secret YAML file')
    String constantsConfig = new File("${BOKCHOY_DB_CACHE_UPLOADER_SECRET}").text
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
Example secret YAML file used by this script
bokchoyDbCacheUploaderSecret:
    toolsTeam: [ member1, member2, ... ]
    accessKeyId : 123abc
    secretAccessKey : 123abc
    region : us-east-1
    email : email-address@email.com
*/

// Iterate over the job configurations
secretMap.each { jobConfigs ->
    Map jobConfig = jobConfigs.getValue()

    assert jobConfig.containsKey('toolsTeam')
    assert jobConfig.containsKey('accessKeyId')
    assert jobConfig.containsKey('secretAccessKey')
    assert jobConfig.containsKey('region')
    assert jobConfig.containsKey('email')

    job('bokchoy-db-cache-uploader') {

        description('Check to see if a merge introduces new migrations, and create a PR into edx-platform if it does.')

        // Enable project security to avoid exposing aws keys
        authorization {
            blocksInheritance(true)
            jobConfig['toolsTeam'].each { member ->
                permissionAll(member)
            }
        }

        properties {
              githubProjectUrl(JENKINS_PUBLIC_GITHUB_BASEURL + "edx/edx-platform")
        }
        logRotator JENKINS_PUBLIC_LOG_ROTATOR()
        label('jenkins-worker')
        concurrentBuild(false)

        multiscm {
           git {
                remote {
                    url(JENKINS_PUBLIC_GITHUB_BASEURL + 'edx/edx-platform.git')
                    refspec('+refs/heads/master:refs/remotes/origin/master')
                }
                branch('master')
                browser()
                extensions {
                    cloneOptions {
                        reference("\$HOME/" + 'clone/.git')
                        timeout(10)
                    }
                    cleanBeforeCheckout()
                    relativeTargetDirectory('edx-platform')
                }
            }
            git {
                remote {
                    url(JENKINS_PUBLIC_GITHUB_BASEURL + 'edx/testeng-ci.git')
                }
                branch('master')
                browser()
                extensions {
                    cleanBeforeCheckout()
                    relativeTargetDirectory('testeng-ci')
                }
            }
        }
        triggers {
            // Trigger jobs via github pushes
            githubPush()
        }

        // Put sensitive info into masked password slots
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

        environmentVariables {
            env('AWS_DEFAULT_REGION', jobConfig['region'])
            env('PYTHON_VERSION', '3.8')
        }

        wrappers {
            timeout {
                absolute(20)
            }
            credentialsBinding {
                string('GITHUB_TOKEN', 'GITHUB_CACHE_UPLOADER_TOKEN')
                string('GITHUB_USER_EMAIL', 'GITHUB_CACHE_UPLOADER_EMAIL')
            }
            timestamps()
        }

        steps {
            shell(readFileFromWorkspace('testeng/resources/bokchoy-db-cache-script.sh'))
        }

        Map <String, String> predefinedPropsMap  = [:]
        predefinedPropsMap.put('GIT_SHA', '${GIT_COMMIT}')
        predefinedPropsMap.put('GITHUB_REPO', 'edx-platform')

        publishers {
            mailer(jobConfig['email'])
            configure GENERAL_SLACK_STATUS()
        }
    }
}

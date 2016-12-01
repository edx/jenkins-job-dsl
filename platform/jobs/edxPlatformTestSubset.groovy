package devops

import org.yaml.snakeyaml.Yaml
import static org.edx.jenkins.dsl.JenkinsPublicConstants.JENKINS_PUBLIC_ARCHIVE_ARTIFACTS
import static org.edx.jenkins.dsl.JenkinsPublicConstants.JENKINS_PUBLIC_ARCHIVE_XUNIT
import static org.edx.jenkins.dsl.JenkinsPublicConstants.JENKINS_PUBLIC_WORKER
import static org.edx.jenkins.dsl.JenkinsPublicConstants.JENKINS_PUBLIC_GITHUB_BASEURL

/*
Example secret YAML file used by this script
publicJobConfig:
    open : true/false
    jobName : name-of-jenkins-job-to-be
    protocol : protocol-and-base-url
    url : github-url-segment
    credential : n/a
    cloneReference : clone/.git
*/

/* stdout logger */
/* use this instead of println, because you can pass it into closures or other scripts. */
/* TODO: Move this into JenkinsPublicConstants, as it can be shared. */
Map config = [:]
Binding bindings = getBinding()
config.putAll(bindings.getVariables())
PrintStream out = config['out']

stringParams = [
    [
        name: 'sha1',
        description: 'Sha1 hash of branch to build. Default branch : master',
        default: 'master'
    ],
    [
        name: 'TEST_SUITE',
        description: '',
        default: ''
    ],
    [
        name: 'SHARD',
        description: '',
        default: ''
    ],
    [
        name: 'PARENT_BUILD',
        description: 'Solution to <a href=\"https://openedx.atlassian.net/browse/TE-894\">TE-894</a>. ' +
                     'Leave as default if starting a build manually.',
        default: '0'
    ],
    [
        name: 'ENV_VARS',
        description: '',
        default: ''
    ]
]

/* Groovy script called within job to process environment variables for easier use */
String envVarScript = readFileFromWorkspace('platform/resources/mapEnvVars.groovy')

Map secretMap = [:]
try {
    out.println('Parsing secret YAML file')
    /* Parse k:v pairs from the secret file referenced by secretFileVariable */
    String contents = new File("${EDX_PLATFORM_TEST_SUBSET_SECRET}").text
    Yaml yaml = new Yaml()
    secretMap = yaml.load(contents)
    out.println('Successfully parsed secret YAML file')
}
catch (any) {
    out.println('Jenkins DSL: Error parsing secret YAML file')
    out.println('Exiting with error code 1')
    return 1
}

/* Iterate over the job configurations */
secretMap.each { jobConfigs ->

    Map jobConfig = jobConfigs.getValue()

    /* Test secret contains all necessary keys for this job */
    assert jobConfig.containsKey('open')
    assert jobConfig.containsKey('jobName')
    assert jobConfig.containsKey('url')
    assert jobConfig.containsKey('credential')
    assert jobConfig.containsKey('cloneReference')

    /* Actual DSL component */
    job(jobConfig['jobName']) {

        logRotator {
            daysToKeep(14)
            numToKeep(-1)
            artifactDaysToKeep(5)
            artifactNumToKeep(-1)
        }
        properties {
            githubProjectUrl(JENKINS_PUBLIC_GITHUB_BASEURL + jobConfig['url'])
        }

        /* For non-open jobs, enable project based security */
        if (!jobConfig['open'].toBoolean()) {
            authorization {
                blocksInheritance(true)
                permissionAll('edx')
            }
        }

        /* Populate job parameters (String parameters in this case) */
        parameters {
            stringParams.each { param ->
                stringParam(param.name, param.default, param.description)
            }
            labelParam('WORKER_LABEL') {
                description('Select a Jenkins worker label for running this job')
                defaultValue(JENKINS_PUBLIC_WORKER)
            }
        }

        concurrentBuild(true)

        /*  configure project to pull from a github repo */
        scm {
            git {
                remote {
                    url(jobConfig['protocol'] + jobConfig['url'] + '.git')
                    refspec('+refs/heads/*:refs/remotes/origin/* +refs/pull/*:refs/remotes/origin/pr/*')
                    if (!jobConfig['open'].toBoolean()) {
                        credentials(jobConfig['credential'])
                    }
                }
                browser()
                branch('\${sha1}')
                extensions {
                    cloneOptions {
                        shallow(false)
                        reference('\$HOME/'.concat(jobConfig['cloneReference']))
                        /* Timeout (in minutes) for cloning from github */
                        timeout(10)
                    }
                    cleanBeforeCheckout()
                }
            }
        }

        /* extra configurations for build environment */
        wrappers {
            timeout {
                absolute(75)
                writeDescription('')
                abortBuild()
            }
            timestamps()
            colorizeOutput('gnome-terminal')
            environmentVariables {
                groovy(envVarScript)
            }
            if (!jobConfig['open'].toBoolean()) {
                sshAgent(jobConfig['credential'])
            }
            buildName('#\${BUILD_NUMBER}: \${ENV,var=\"TEST_SUITE\"} \${ENV,var=\"SHARD\"}')
        }

        /* Actual build steps for this job */
        steps {
            shell('bash scripts/all-tests.sh')
        }

        /* Publish artifacts from build */
        publishers {
            archiveArtifacts JENKINS_PUBLIC_ARCHIVE_ARTIFACTS()
            archiveXUnit JENKINS_PUBLIC_ARCHIVE_XUNIT()
        }
    }
}

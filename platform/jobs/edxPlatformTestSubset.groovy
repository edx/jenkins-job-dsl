package devops

import hudson.model.Build
import static org.edx.jenkins.dsl.JenkinsPublicConstants.JENKINS_PUBLIC_LOG_ROTATOR
import static org.edx.jenkins.dsl.JenkinsPublicConstants.JENKINS_PUBLIC_ARCHIVE_ARTIFACTS
import static org.edx.jenkins.dsl.JenkinsPublicConstants.JENKINS_PUBLIC_ARCHIVE_XUNIT
import static org.edx.jenkins.dsl.JenkinsPublicConstants.JENKINS_PUBLIC_WORKER
import static org.edx.jenkins.dsl.JenkinsPublicConstants.JENKINS_PUBLIC_PARSE_SECRET

/*
Example secret YAML file used by this script
publicJobConfig:
    open : true/false
    jobName : name-of-jenkins-job-to-be
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
String envVarScript = readFileFromWorkspace('resources/mapEnvVars.groovy')

/* Environment variable (set in Seeder job config) to reference a Jenkins secret file */
String secretFileVariable = 'EDX_PLATFORM_TEST_SUBSET_SECRET'

def secretMap = [:]
try {
    out.println('Parsing secret YAML file')
    /* Parse k:v pairs from the secret file referenced by secretFileVariable */
    Thread thread = Thread.currentThread()
    Build build = thread?.executable
    Map envVarsMap = build.parent.builds[0].properties.get("envVars")
    secretMap = JENKINS_PUBLIC_PARSE_SECRET.call(secretFileVariable, envVarsMap, out)
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
    /* TODO: Use/Build a more robust test framework for this */
    assert jobConfig.containsKey('open')
    assert jobConfig.containsKey('jobName')
    assert jobConfig.containsKey('url')
    assert jobConfig.containsKey('credential')
    assert jobConfig.containsKey('cloneReference')

    /* Actual DSL component */
    job(jobConfig['jobName']) {

        logRotator JENKINS_PUBLIC_LOG_ROTATOR()
        properties {
            githubProjectUrl('https://github.com/'.concat(jobConfig['url']))
        }
        
        /* For open jobs, enable project based security so viewing is public */
        if (jobConfig['open'].toBoolean())  {
            authorization {
                permission('hudson.model.Item.Read', 'anonymous')
            }
        }

        /* Populate job parameters (String parameters in this case) */
        parameters {
            stringParams.each { param ->
                stringParam(param.name, param.default, param.description)
            }
        }

        concurrentBuild(true)
        label(JENKINS_PUBLIC_WORKER)

        /*  configure project to pull from a github repo */
        scm {
            git {
                remote {
                    github(jobConfig['url'], 'https', 'github.com')
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

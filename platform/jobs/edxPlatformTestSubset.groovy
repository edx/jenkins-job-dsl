package devops

import org.yaml.snakeyaml.Yaml
import static org.edx.jenkins.dsl.JenkinsPublicConstants.JENKINS_PUBLIC_ARCHIVE_ARTIFACTS
import static org.edx.jenkins.dsl.JenkinsPublicConstants.JENKINS_PUBLIC_ARCHIVE_XUNIT
import static org.edx.jenkins.dsl.JenkinsPublicConstants.JENKINS_PUBLIC_LOG_ROTATOR
import static org.edx.jenkins.dsl.JenkinsPublicConstants.JENKINS_PUBLIC_WORKER
import static org.edx.jenkins.dsl.JenkinsPublicConstants.JENKINS_PUBLIC_GITHUB_BASEURL

/* stdout logger */
/* use this instead of println, because you can pass it into closures or other scripts. */
/* TODO: Move this into JenkinsPublicConstants, as it can be shared. */
Map config = [:]
Binding bindings = getBinding()
config.putAll(bindings.getVariables())
PrintStream out = config['out']

/*
Example secret YAML file used by this script
Map exampleConfig = [
    open : true/false
    jobName : name-of-jenkins-job-to-be
    url : github-url-segment
    cloneReference : clone/.git
]
*/

Map publicJobConfig = [
    open : true,
    jobName : 'edx-platform-test-subset',
    url : 'raccoongang/edx-platform-test',
    cloneReference : 'edx-platform-clone/.git'
]

List jobConfigs = [
    publicJobConfig,
]

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

/* Iterate over the job configurations */
jobConfigs.each { jobConfig ->

    job(jobConfig.jobName) {

        logRotator JENKINS_PUBLIC_LOG_ROTATOR(7, -1, 3, -1)
        properties {
            githubProjectUrl(JENKINS_PUBLIC_GITHUB_BASEURL + jobConfig.url)
        }

        /* For non-open jobs, enable project based security */
        if (!jobConfig.open.toBoolean()) {
            authorization {
                blocksInheritance(true)
                permissionAll('raccoongang')
                permission('hudson.model.Item.Discover', 'anonymous')
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
	disabled()
        concurrentBuild(true)

        /*  configure project to pull from a github repo */
        scm {
            git {
                remote {
                    url('git@github.com:' + jobConfig.url + '.git')
                    refspec('+refs/heads/*:refs/remotes/origin/* +refs/pull/*:refs/remotes/origin/pr/*')
                    credentials('jenkins-worker')
                }
                browser()
                branch('\${sha1}')
                extensions {
                    cloneOptions {
                        shallow(false)
                        reference('\$HOME/'.concat(jobConfig.cloneReference))
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
                groovy(readFileFromWorkspace('platform/resources/mapEnvVars.groovy'))
            }
            buildName('#\${BUILD_NUMBER}: \${ENV,var=\"TEST_SUITE\"} \${ENV,var=\"SHARD\"}')
            credentialsBinding {
                string('AWS_ACCESS_KEY_ID', 'DB_CACHE_ACCESS_KEY_ID')
                string('AWS_SECRET_ACCESS_KEY', 'DB_CACHE_SECRET_ACCESS_KEY')
            }
        }

        /* Actual build steps for this job */
        steps {
            shell(readFileFromWorkspace('platform/resources/quality-fail-fast.sh'))
            shell('bash scripts/all-tests.sh')
        }

        /* Publish artifacts from build */
        publishers {
            archiveArtifacts JENKINS_PUBLIC_ARCHIVE_ARTIFACTS()
            archiveXUnit JENKINS_PUBLIC_ARCHIVE_XUNIT()
        }
    }
}

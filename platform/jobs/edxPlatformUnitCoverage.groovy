package platform

import org.yaml.snakeyaml.Yaml
import static org.edx.jenkins.dsl.JenkinsPublicConstants.GENERAL_PRIVATE_JOB_SECURITY
import static org.edx.jenkins.dsl.JenkinsPublicConstants.JENKINS_PUBLIC_LOG_ROTATOR

stringParams = [
    [
        name: 'sha1',
        description: '',
        default: ''
    ],
    [
        name: 'UNIT_BUILD_NUM_1',
        description: '',
        default: ''
    ],
    [
        name: 'UNIT_BUILD_NUM_2',
        description: '',
        default: ''
    ],
    [
        name: 'UNIT_BUILD_NUM_3',
        description: '',
        default: ''
    ],
    [
        name: 'UNIT_BUILD_NUM_4',
        description: '',
        default: ''
    ],
    [
        name: 'UNIT_BUILD_NUM_5',
        description: '',
        default: ''
    ],
    [
        name: 'UNIT_BUILD_NUM_6',
        description: '',
        default: ''
    ],
    [
        name: 'UNIT_BUILD_NUM_7',
        description: '',
        default: ''
    ],
    [
        name: 'UNIT_BUILD_NUM_8',
        description: '',
        default: ''
    ],
    [
        name: 'UNIT_BUILD_NUM_9',
        description: '',
        default: ''
    ],
    [
        name: 'UNIT_BUILD_NUM_10',
        description: '',
        default: ''
    ],
    [
        name: 'UNIT_BUILD_NUM_11',
        description: '',
        default: ''
    ],
    [
        name: 'UNIT_BUILD_NUM_12',
        description: '',
        default: ''
    ],
    [
        name: 'UNIT_BUILD_NUM_13',
        description: '',
        default: ''
    ],
    [
        name: 'UNIT_BUILD_NUM_14',
        description: '',
        default: ''
    ],
    [
        name: 'UNIT_BUILD_NUM_15',
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
        name: 'CI_BRANCH',
        description: '',
        default: 'unspecified'
    ],
    [
        name: 'TARGET_BRANCH',
        description: 'Branch of the edx-platform to run diff-coverage against',
        default: 'origin/master'
    ]
]

// This script generates a lot of jobs. Here is the breakdown of the configuration options:
// Map exampleConfig = [ open: true/false if this job should be 'open' (use the default security scheme or not)
//                       jobName: name of the job
//                       subsetJob: name of the subset job on which the parent job has run unit tests
//                       repoName: name of the github repo containing the edx-platform you want to test
//                       workerLabel: label of the worker to run this job on
//                       ]

Map publicJobConfig = [ open : true,
                        jobName : 'edx-platform-unit-coverage',
                        subsetJob: 'edx-platform-test-subset',
                        repoName: 'edx-platform',
                        workerLabel: 'coverage-worker',
                        timeout: 20
                        ]

List jobConfigs = [ publicJobConfig,
                    ]

/* Iterate over the job configurations */
jobConfigs.each { jobConfig ->

    job(jobConfig.jobName) {

        logRotator JENKINS_PUBLIC_LOG_ROTATOR(1)

        if (!jobConfig.open.toBoolean()) {
            authorization GENERAL_PRIVATE_JOB_SECURITY()
        }
        parameters {
            stringParams.each { param ->
                stringParam(param.name, param.default, param.description)
            }
        }
        concurrentBuild(true)
        label(jobConfig.workerLabel)
        environmentVariables {
            env('SUBSET_JOB', jobConfig.subsetJob)
        }
        scm {
            git {
                remote {
                    url("git@github.com:edx/${jobConfig.repoName}.git")
                    refspec('+refs/pull/*:refs/remotes/origin/pr/*')
                    credentials('jenkins-worker')
                }
                branch('\${sha1}')
                browser()
                extensions {
                    cloneOptions {
                        shallow(false)
                        // Use a reference clone for quicker clones. This is configured on jenkins workers via
                        // (https://github.com/edx/configuration/blob/master/playbooks/roles/test_build_server/tasks/main.yml#L26)
                        reference("\$HOME/edx-platform-clone")
                        timeout(10)
                    }
                    cleanBeforeCheckout()
                }
            }
        }
        wrappers {
            sshAgent('jenkins-worker')
            timeout {
                absolute(jobConfig.timeout)
                writeDescription("Timed out at ${jobConfig.timeout} minutes")
                abortBuild()
            }
            timestamps()
            colorizeOutput('gnome-terminal')
            if (!jobConfig.open.toBoolean()) {
                sshAgent('jenkins-worker')
            }
            buildName('#\${BUILD_NUMBER}: \${GIT_REVISION,length=8}')
            // Inject CodeCov token so that public jobs can report coverage
            if (jobConfig.open.toBoolean()) {
                credentialsBinding {
                    string('CODE_COV_TOKEN', 'CODE_COV_TOKEN')
                }
            }
        }
        /* Copy Artifacts from test subset jobs with build number UNIT_BUILD_NUM */
        steps {
            for (buildNum = 1; buildNum < 16; buildNum += 1) {
                copyArtifacts(jobConfig.subsetJob) {
                    buildSelector {
                        buildNumber("\$UNIT_BUILD_NUM_".concat(buildNum.toString()))
                    }
                    includePatterns('reports/**/*coverage*')
                    fingerprintArtifacts(false)
                    if (buildNum > 2) {
                         optional()
                    }
                }
            }
            // Run jenkins-report.sh which will upload coverage results to
            // codecov.
            shell('./scripts/jenkins-report.sh')
        }
        publishers {
            archiveArtifacts {
                pattern('reports/diff_coverage_combined.html,reports/**')
                defaultExcludes()
            }
        }
    }
}

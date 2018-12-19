package platform

import org.yaml.snakeyaml.Yaml
import static org.edx.jenkins.dsl.JenkinsPublicConstants.GENERAL_PRIVATE_JOB_SECURITY
import static org.edx.jenkins.dsl.JenkinsPublicConstants.JENKINS_PUBLIC_ARCHIVE_XUNIT
import static org.edx.jenkins.dsl.JenkinsPublicConstants.JENKINS_PUBLIC_LOG_ROTATOR

String deleteReports = 'reports/**/*,test_root/log/*.log,'
deleteReports += 'edx-platform*/reports/**/*,edx-platform*/test_root/log/*.log,'

stringParams = [
    [
        name: 'sha1',
        description: '',
        default: ''
    ],
    [
        name: 'QUALITY_BUILD_NUM_1',
        description: '',
        default: ''
    ],
    [
        name: 'QUALITY_BUILD_NUM_2',
        description: '',
        default: ''
    ],
    [
        name: 'QUALITY_BUILD_NUM_3',
        description: '',
        default: ''
    ],
    [
        name: 'QUALITY_BUILD_NUM_4',
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
        description: 'Branch of the edx-platform to run diff-cover against',
        default: 'origin/master'
    ]
]

Map publicJobConfig = [
    open : true,
    jobName : 'edx-platform-quality-diff',
    subsetJob: 'edx-platform-test-subset',
    repoName: 'edx-platform'
]


List jobConfigs = [
    publicJobConfig,
]

jobConfigs.each { jobConfig ->

    job(jobConfig.jobName) {

        logRotator JENKINS_PUBLIC_LOG_ROTATOR(7)

        if (!jobConfig.open.toBoolean()) {
            authorization GENERAL_PRIVATE_JOB_SECURITY()
        }
        parameters {
            stringParams.each { param ->
                stringParam(param.name, param.default, param.description)
            }
        }
        concurrentBuild(true)
        label('coverage-worker')
        environmentVariables {
            env('SUBSET_JOB', jobConfig.subsetJob)
        }
        scm {
            git {
                remote {
                    url("git@github.com:edx/${jobConfig.repoName}.git")
                    refspec('+refs/pull/*:refs/remotes/origin/pr/*')
                    if (!jobConfig.open.toBoolean()) {
                        credentials('jenkins-worker')
                    }
                }
                branch('\${sha1}')
                browser()
                extensions {
                    cloneOptions {
                        shallow(false)
                        reference("\$HOME/edx-platform-clone")
                        timeout(10)
                    }
                    cleanBeforeCheckout()
                }
            }
        }
        wrappers {
            timeout {
                absolute(20)
                writeDescription('Timed out at 20 minutes')
                abortBuild()
            }
            timestamps()
            colorizeOutput('gnome-terminal')
            preBuildCleanup {
                includePattern(deleteReports)
                deleteDirectories()
            }
            sshAgent('jenkins-worker')
            buildName('#\${BUILD_NUMBER}: \${GIT_REVISION,length=8}')
        }
        steps {
            for (buildNum = 1; buildNum <= 4; buildNum += 1) {
                copyArtifacts(jobConfig.subsetJob) {
                    buildSelector {
                        buildNumber("\$QUALITY_BUILD_NUM_".concat(buildNum.toString()))
                    }
                    includePatterns(
                        'reports/**/*.report', 'reports/**/*.log',
                        'edx-platform*/reports/**/*.report', 'edx-platform*/reports/**/*.log',
                    )
                    fingerprintArtifacts(false)
                }
            }
            // Gather all the linter outputs from the shards and check against the
            // absolute and diff limits that we have set.
            shell('./scripts/jenkins-quality-diff.sh')
        }
        publishers {
            archiveArtifacts {
                pattern('test_root/log/*.log,reports/*.html,reports/**/*.html,reports/**/*.xml')
                defaultExcludes(true)
            }
            archiveXUnit JENKINS_PUBLIC_ARCHIVE_XUNIT()
            publishHtml {
                report("${jobConfig.repoName}/reports/metrics/") {
                    reportName('Quality Report')
                    reportFiles(
                        'pylint/*view*/,pep8/*view*/,python_complexity/*view*/,' +
                        'xsscommitlint/*view*/,xsslint/*view*/,eslint/*view*/'
                    )
                    keepAll(true)
                    allowMissing(true)
                }
                report("${jobConfig.repoName}/reports/diff_quality") {
                    reportName('Diff Quality Report')
                    reportFiles('diff_quality_pylint.html, diff_quality_eslint.html')
                    keepAll(true)
                    allowMissing(true)
                }
            }
        }
    }
}

package platform

import org.yaml.snakeyaml.Yaml
import static org.edx.jenkins.dsl.JenkinsPublicConstants.JENKINS_PUBLIC_LOG_ROTATOR
import static org.edx.jenkins.dsl.JenkinsPublicConstants.JENKINS_PUBLIC_GITHUB_BASEURL
import static org.edx.jenkins.dsl.JenkinsPublicConstants.GHPRB_WHITELIST_BRANCH
import static org.edx.jenkins.dsl.JenkinsPublicConstants.GENERAL_PRIVATE_JOB_SECURITY

/* stdout logger */
/* use this instead of println, because you can pass it into closures or other scripts. */
Map config = [:]
Binding bindings = getBinding()
config.putAll(bindings.getVariables())
PrintStream out = config['out']

/* Map to hold the k:v pairs parsed from the secret file */
Map ghprbMap = [:]
try {
    out.println('Parsing secret YAML file')
    /* Parse k:v pairs from the secret file referenced by secretFileVariable */
    String ghprbConfigContents = new File("${GHPRB_SECRET}").text
    Yaml yaml = new Yaml()
    ghprbMap = yaml.load(ghprbConfigContents)
    out.println('Successfully parsed secret YAML file')
}
catch (any) {
    out.println('Jenkins DSL: Error parsing secret YAML file')
    out.println('Exiting with error code 1')
    return 1
}

// This script generates a lot of jobs. Here is the breakdown of the configuration options:
// Map exampleConfig = [ open: true/false if this job should be 'open' (use the default security scheme or not)
//                       jobName: name of the job
//                       repoName: name of the github repo containing the edx-platform you want to test
//                       workerLabel: label of the worker to run this job on
//                       whiteListBranchRegex: regular expression to filter which branches of a particular repo
//                       can will trigger builds (via GHRPB)
//                       context: Github context used to report test status
//                       triggerPhrase: Github comment used to trigger this job
//                       ]

Map publicJobConfig = [ open : true,
                        jobName : 'edx-platform-quality-pr',
                        subsetJob: 'edx-platform-test-subset',
                        repoName: 'edx-platform',
                        workerLabel: 'jenkins-worker',
                        whitelistBranchRegex: /^((?!open-release\/).)*$/,
                        context: 'jenkins/quality',
                        triggerPhrase: 'jenkins run quality'
                        ]

Map privateJobConfig = [ open: false,
                         jobName: 'edx-platform-quality-pr_private',
                         repoName: 'edx-platform-private',
                         workerLabel: 'jenkins-worker',
                         whitelistBranchRegex: /^((?!open-release\/).)*$/,
                         context: 'jenkins/quality',
                         triggerPhrase: 'jenkins run quality'
                         ]

Map publicGinkgoJobConfig = [ open: true,
                              jobName: 'ginkgo-quality-pr',
                              repoName: 'edx-platform',
                              workerLabel: 'ginkgo-jenkins-worker',
                              whitelistBranchRegex: /open-release\/ginkgo.master/,
                              context: 'jenkins/ginkgo/quality',
                              triggerPhrase: 'ginkgo run quality'
                              ]

Map privateGinkgoJobConfig = [ open: false,
                               jobName: 'ginkgo-quality-pr_private',
                               repoName: 'edx-platform-private',
                               workerLabel: 'ginkgo-jenkins-worker',
                               whitelistBranchRegex: /open-release\/ginkgo.master/,
                               context: 'jenkins/ginkgo/quality',
                               triggerPhrase: 'ginkgo run quality'
                               ]

Map publicFicusJobConfig = [ open: true,
                             jobName: 'ficus-quality-pr',
                             repoName: 'edx-platform',
                             workerLabel: 'ficus-jenkins-worker',
                             whitelistBranchRegex: /open-release\/ficus.master/,
                             context: 'jenkins/ficus/quality',
                             triggerPhrase: 'ficus run quality'
                             ]

Map privateFicusJobConfig = [ open: false,
                              jobName: 'ficus-quality-pr_private',
                              repoName: 'edx-platform-private',
                              workerLabel: 'ficus-jenkins-worker',
                              whitelistBranchRegex: /open-release\/ficus.master/,
                              context: 'jenkins/ficus/quality',
                              triggerPhrase: 'ficus run quality'
                              ]

List jobConfigs = [ publicJobConfig,
                    privateJobConfig,
                    publicGinkgoJobConfig,
                    privateGinkgoJobConfig,
                    publicFicusJobConfig,
                    privateFicusJobConfig
                    ]


/* Iterate over the job configurations */
jobConfigs.each { jobConfig ->

    job(jobConfig.jobName) {


        if (!jobConfig.open.toBoolean()) {
            authorization GENERAL_PRIVATE_JOB_SECURITY()
        }
        properties {
              githubProjectUrl("https://github.com/edx/${jobConfig.repoName}/")
        }
        logRotator JENKINS_PUBLIC_LOG_ROTATOR(7)
        concurrentBuild()
        parameters {
            labelParam('WORKER_LABEL') {
                description('Select a Jenkins worker label for running this job')
                defaultValue(jobConfig.workerLabel)
            }
        }
        scm {
            git {
                remote {
                    if (!jobConfig.open.toBoolean()) {
                        url("git@github.com:edx/${jobConfig.repoName}.git")
                    }
                    else {
                        url("https://github.com/edx/${jobConfig.repoName}.git")
                    }
                    refspec('+refs/pull/*:refs/remotes/origin/pr/*')
                    if (!jobConfig.open.toBoolean()) {
                        credentials('jenkins-worker')
                    }
                }
                branch('\${ghprbActualCommit}')
                browser()
                extensions {
                    relativeTargetDirectory(jobConfig.repoName)
                    cloneOptions {
                        // Use a reference clone for quicker clones. This is configured on jenkins workers via
                        // (https://github.com/edx/configuration/blob/master/playbooks/roles/test_build_server/tasks/main.yml#L26)
                        reference("\$HOME/edx-platform-clone")
                        timeout(10)
                    }
                    cleanBeforeCheckout()
                }
            }
        }
        triggers {
            pullRequest {
                admins(ghprbMap['admin'])
                useGitHubHooks()
                triggerPhrase(jobConfig.triggerPhrase)
                userWhitelist(ghprbMap['userWhiteList'])
                orgWhitelist(ghprbMap['orgWhiteList'])
                extensions {
                    commitStatus {
                        context(jobConfig.context)
                    }
                }
            }
        }

        configure GHPRB_WHITELIST_BRANCH(jobConfig.whitelistBranchRegex)

        wrappers {
            timeout {
                absolute(90)
            }
            timestamps()
            colorizeOutput()
            buildName('#\${BUILD_NUMBER}: Quality Tests')
            if (!jobConfig.open.toBoolean()) {
                sshAgent('jenkins-worker')
            }
        }
        steps {
            shell("cd ${jobConfig.repoName}; TEST_SUITE=quality ./scripts/all-tests.sh")
        }
        publishers {
            archiveArtifacts {
                pattern('edx-platform*/reports/**/*,edx-platform*/test_root/log/*.png,edx-platform*/' +
                        'test_root/log/*.log,edx-platform*/**/' +
                        'nosetests.xml,edx-platform*/**/TEST-*.xml')
                defaultExcludes(true)
                allowEmpty(true)
            }
            publishHtml {
                report("${jobConfig.repoName}/reports/metrics/") {
                    reportName('Quality Report')
                    reportFiles('pylint/*view*/,pep8/*view*/,jshint/*view*/,python_complexity/*view*/,' +
                                'xsscommitlint/*view*/,xsslint/*view*/,eslint/*view*/')
                    keepAll(true)
                    allowMissing(true)
                }
                report("${jobConfig.repoName}/reports/diff_quality") {
                    reportName('Diff Quality Report')
                    reportFiles('diff_quality_pep8.html, diff_quality_pylint.html, ' +
                                'diff_quality_jshint.html, diff_quality_eslint.html')
                    keepAll(true)
                    allowMissing(true)
                }
            }
        }
    }
}

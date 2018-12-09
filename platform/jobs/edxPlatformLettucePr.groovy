package platform

import org.yaml.snakeyaml.Yaml
import static org.edx.jenkins.dsl.JenkinsPublicConstants.JENKINS_PUBLIC_LOG_ROTATOR
import static org.edx.jenkins.dsl.JenkinsPublicConstants.JENKINS_PUBLIC_JUNIT_REPORTS
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
//                       subsetjob: name of subset job run by this job (shard jobs)
//                       repoName: name of the github repo containing the edx-platform you want to test
//                       workerLabel: label of the worker to run the subset jobs on
//                       whiteListBranchRegex: regular expression to filter which branches of a particular repo
//                       can will trigger builds (via GHRPB)
//                       context: Github context used to report test status
//                       triggerPhrase: Github comment used to trigger this job
//                       defaultTestengbranch: default branch of the testeng-ci repo for this job
//                       ]

Map publicJobConfig = [ open : true,
                        jobName : 'edx-platform-lettuce-pr',
                        subsetJob: 'edx-platform-test-subset',
                        repoName: 'edx-platform',
                        workerLabel: 'jenkins-worker',
                        whitelistBranchRegex: /^((?!open-release\/).)*$/,
                        context: 'jenkins/lettuce',
                        triggerPhrase: 'jenkins run lettuce',
                        defaultTestengBranch: 'master'
                        ]

Map privateJobConfig = [ open: false,
                         jobName: 'edx-platform-lettuce-pr_private',
                         subsetJob: 'edx-platform-test-subset_private',
                         repoName: 'edx-platform-private',
                         workerLabel: 'jenkins-worker',
                         whitelistBranchRegex: /^((?!open-release\/).)*$/,
                         context: 'jenkins/lettuce',
                         triggerPhrase: 'jenkins run lettuce',
                         defaultTestengBranch: 'master'
                         ]

Map publicGinkgoJobConfig = [ open: true,
                              jobName: 'ginkgo-lettuce-pr',
                              subsetJob: 'edx-platform-test-subset',
                              repoName: 'edx-platform',
                              workerLabel: 'ginkgo-jenkins-worker',
                              whitelistBranchRegex: /open-release\/ginkgo.master/,
                              context: 'jenkins/ginkgo/lettuce',
                              triggerPhrase: 'ginkgo run lettuce',
                              defaultTestengBranch: 'origin/open-release/ginkgo.master'
                              ]

Map privateGinkgoJobConfig = [ open: false,
                               jobName: 'ginkgo-lettuce-pr_private',
                               subsetJob: 'edx-platform-test-subset_private',
                               repoName: 'edx-platform-private',
                               workerLabel: 'ginkgo-jenkins-worker',
                               whitelistBranchRegex: /open-release\/ginkgo.master/,
                               context: 'jenkins/ginkgo/lettuce',
                               triggerPhrase: 'ginkgo run lettuce',
                               defaultTestengBranch: 'origin/open-release/ginkgo.master'
                               ]

Map publicFicusJobConfig = [ open: true,
                             jobName: 'ficus-lettuce-pr',
                             subsetJob: 'edx-platform-test-subset',
                             repoName: 'edx-platform',
                             workerLabel: 'ficus-jenkins-worker',
                             whitelistBranchRegex: /open-release\/ficus.master/,
                             context: 'jenkins/ficus/lettuce',
                             triggerPhrase: 'ficus run lettuce',
                             defaultTestengBranch: 'origin/open-release/ficus.master'
                             ]

Map privateFicusJobConfig = [ open: false,
                              jobName: 'ficus-lettuce-pr_private',
                              subsetJob: 'edx-platform-test-subset_private',
                              repoName: 'edx-platform-private',
                              workerLabel: 'ficus-jenkins-worker',
                              whitelistBranchRegex: /open-release\/ficus.master/,
                              context: 'jenkins/ficus/lettuce',
                              triggerPhrase: 'ficus run lettuce',
                              defaultTestengBranch: 'origin/open-release/ficus.master'
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

    buildFlowJob(jobConfig.jobName) {

        if (!jobConfig.open.toBoolean()) {
            authorization GENERAL_PRIVATE_JOB_SECURITY()
        }
        properties {
              githubProjectUrl("https://github.com/edx/${jobConfig.repoName}/")
        }
        logRotator JENKINS_PUBLIC_LOG_ROTATOR()
        concurrentBuild()
        label('flow-worker-lettuce')
        checkoutRetryCount(5)
        environmentVariables {
            env('SUBSET_JOB', jobConfig.subsetJob)
            env('REPO_NAME', jobConfig.repoName)
        }
        parameters {
            stringParam('WORKER_LABEL', jobConfig.workerLabel, 'Jenkins worker for running the test subset jobs')
        }
        multiscm {
            git {
                remote {
                    url('https://github.com/edx/testeng-ci.git')
                }
                branch(jobConfig.defaultTestengBranch)
                browser()
                extensions {
                    cleanBeforeCheckout()
                    relativeTargetDirectory('testeng-ci')
                }
            }
           git {
                remote {
                    url("https://github.com/edx/${jobConfig.repoName}.git")
                    refspec('+refs/pull/*:refs/remotes/origin/pr/*')
                    if (!jobConfig.open.toBoolean()) {
                        credentials('EDX_STATUS_BOT_CREDENTIALS')
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

        wrappers {
            timestamps()
        }

        configure GHPRB_WHITELIST_BRANCH(jobConfig.whitelistBranchRegex)

        dslFile('testeng-ci/jenkins/flow/pr/edx-platform-lettuce-pr.groovy')
        publishers {
            archiveJunit(JENKINS_PUBLIC_JUNIT_REPORTS)
        }
    }
}

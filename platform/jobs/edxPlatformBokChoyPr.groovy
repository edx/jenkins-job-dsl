package platform

import org.yaml.snakeyaml.Yaml
import static org.edx.jenkins.dsl.JenkinsPublicConstants.JENKINS_PUBLIC_LOG_ROTATOR
import static org.edx.jenkins.dsl.JenkinsPublicConstants.JENKINS_PUBLIC_JUNIT_REPORTS
import static org.edx.jenkins.dsl.JenkinsPublicConstants.GHPRB_CANCEL_BUILDS_ON_UPDATE
import static org.edx.jenkins.dsl.JenkinsPublicConstants.GENERAL_PRIVATE_JOB_SECURITY
import static org.edx.jenkins.dsl.JenkinsPublicConstants.JENKINS_EDX_PLATFORM_TEST_NOTIFIER

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
//                       can trigger builds (via GHRPB)
//                       should not trigger builds (via GHRPB)
//                       context: Github context used to report test status
//                       triggerPhrase: Github comment used to trigger this job
//                       defaultTestengbranch: default branch of the testeng-ci repo for this job
//                       commentOnly: true/false if this job should only be triggered by explicit comments on
//                       github. Default behavior: triggered by comments AND pull request updates
//                       ]

Map publicJobConfig = [ open : true,
                        jobName : 'edx-platform-bok-choy-pr',
                        subsetJob: 'edx-platform-test-subset',
                        repoName: 'edx-platform',
                        workerLabel: 'jenkins-worker',
                        whitelistBranchRegex: /^((?!open-release\/).)*$/,
                        context: 'jenkins/bokchoy',
                        triggerPhrase: /.*jenkins\W+run\W+bokchoy.*/,
                        defaultTestengBranch: 'master'
                        ]

Map privateJobConfig = [ open: false,
                         jobName: 'edx-platform-bok-choy-pr_private',
                         subsetJob: 'edx-platform-test-subset_private',
                         repoName: 'edx-platform-private',
                         workerLabel: 'jenkins-worker',
                         whitelistBranchRegex: /^((?!open-release\/).)*$/,
                         context: 'jenkins/bokchoy',
                         triggerPhrase: /.*jenkins\W+run\W+bokchoy.*/,
                         defaultTestengBranch: 'master'
                         ]

Map publicHawthornJobConfig = [ open: true,
                               jobName: 'hawthorn-bok-choy-pr',
                               subsetJob: 'edx-platform-test-subset',
                               repoName: 'edx-platform',
                               workerLabel: 'hawthorn-jenkins-worker',
                               whitelistBranchRegex: /open-release\/hawthorn.beta1/,
                               context: 'jenkins/hawthorn/bokchoy',
                               triggerPhrase: /.*hawthorn\W+run\W+bokchoy.*/,
                               defaultTestengBranch: 'origin/open-release/hawthorn.beta1'
                               ]

Map privateHawthornJobConfig = [ open: false,
                                jobName: 'hawthorn-bok-choy-pr_private',
                                subsetJob: 'edx-platform-test-subset_private',
                                repoName: 'edx-platform-private',
                                workerLabel: 'hawthorn-jenkins-worker',
                                whitelistBranchRegex: /open-release\/hawthorn.beta1/,
                                context: 'jenkins/hawthorn/bokchoy',
                                triggerPhrase: /.*hawthorn\W+run\W+bokchoy.*/,
                                defaultTestengBranch: 'origin/open-release/hawthorn.beta1'
                                ]

Map publicGinkgoJobConfig = [ open: true,
                              jobName: 'ginkgo-bok-choy-pr',
                              subsetJob: 'edx-platform-test-subset',
                              repoName: 'edx-platform',
                              workerLabel: 'ginkgo-jenkins-worker',
                              whitelistBranchRegex: /open-release\/ginkgo.master/,
                              context: 'jenkins/ginkgo/bokchoy',
                              triggerPhrase: /.*ginkgo\W+run\W+bokchoy.*/,
                              defaultTestengBranch: 'origin/open-release/ginkgo.master'
                              ]

Map privateGinkgoJobConfig = [ open: false,
                               jobName: 'ginkgo-bok-choy-pr_private',
                               subsetJob: 'edx-platform-test-subset_private',
                               repoName: 'edx-platform-private',
                               workerLabel: 'ginkgo-jenkins-worker',
                               whitelistBranchRegex: /open-release\/ginkgo.master/,
                               context: 'jenkins/ginkgo/bokchoy',
                               triggerPhrase: /.*ginkgo\W+run\W+bokchoy.*/,
                               defaultTestengBranch: 'origin/open-release/ginkgo.master'
                               ]

Map publicFicusJobConfig = [ open: true,
                             jobName: 'ficus-bok-choy-pr',
                             subsetJob: 'edx-platform-test-subset',
                             repoName: 'edx-platform',
                             workerLabel: 'ficus-jenkins-worker',
                             whitelistBranchRegex: /open-release\/ficus.master/,
                             context: 'jenkins/ficus/bokchoy',
                             triggerPhrase: /.*ficus\W+run\W+bokchoy.*/,
                             defaultTestengBranch: 'origin/open-release/ficus.master'
                             ]

Map privateFicusJobConfig = [ open: false,
                              jobName: 'ficus-bok-choy-pr_private',
                              subsetJob: 'edx-platform-test-subset_private',
                              repoName: 'edx-platform-private',
                              workerLabel: 'ficus-jenkins-worker',
                              whitelistBranchRegex: /open-release\/ficus.master/,
                              context: 'jenkins/ficus/bokchoy',
                              triggerPhrase: /.*ficus\W+run\W+bokchoy.*/,
                              defaultTestengBranch: 'origin/open-release/ficus.master'
                              ]

Map python3JobConfig = [ open : true,
                         jobName : 'edx-platform-python3-bok-choy-pr',
                         subsetJob: 'edx-platform-test-subset',
                         repoName: 'edx-platform',
                         workerLabel: 'jenkins-worker',
                         whitelistBranchRegex: /^((?!open-release\/).)*$/,
                         context: 'jenkins/python3.5/bokchoy',
                         triggerPhrase: /.*jenkins\W+run\W+py35-django111\W+bokchoy.*/,
                         defaultTestengBranch: 'master',
                         commentOnly: true,
                         toxEnv: 'py35-django111'
                         ]

List jobConfigs = [ publicJobConfig,
                    privateJobConfig,
                    publicHawthornJobConfig,
                    privateHawthornJobConfig,
                    publicGinkgoJobConfig,
                    privateGinkgoJobConfig,
                    publicFicusJobConfig,
                    privateFicusJobConfig,
                    python3JobConfig
                    ]

/* Iterate over the job configurations */
jobConfigs.each { jobConfig ->

    buildFlowJob(jobConfig['jobName']) {

        if (!jobConfig.open.toBoolean()) {
            authorization GENERAL_PRIVATE_JOB_SECURITY()
        }
        properties {
              githubProjectUrl("https://github.com/edx/${jobConfig.repoName}/")
        }
        logRotator JENKINS_PUBLIC_LOG_ROTATOR(7)
        concurrentBuild()
        label('flow-worker-bokchoy')
        checkoutRetryCount(5)
        environmentVariables {
            env('SUBSET_JOB', jobConfig.subsetJob)
            env('REPO_NAME', jobConfig.repoName)
            env('TOX_ENV', jobConfig.toxEnv)
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
        }

        triggers {
            githubPullRequest {
                admins(ghprbMap['admin'])
                useGitHubHooks()
                triggerPhrase(jobConfig.triggerPhrase)
                if (jobConfig.commentOnly) {
                    onlyTriggerPhrase(true)
                }
                userWhitelist(ghprbMap['userWhiteList'])
                orgWhitelist(ghprbMap['orgWhiteList'])
                whiteListTargetBranches([jobConfig.whitelistBranchRegex])
                extensions {
                    commitStatus {
                        context(jobConfig.context)
                    }
                }
            }
        }
        configure GHPRB_CANCEL_BUILDS_ON_UPDATE(false)

        wrappers {
            timestamps()
        }

        dslFile('testeng-ci/jenkins/flow/pr/edx-platform-bok-choy-pr.groovy')
        publishers { //publish JUnit Test report
            archiveJunit(JENKINS_PUBLIC_JUNIT_REPORTS)
            if (jobConfig.repoName == "edx-platform") {
                downstreamParameterized JENKINS_EDX_PLATFORM_TEST_NOTIFIER('${ghprbPullId}')
            }
        }
    }
}

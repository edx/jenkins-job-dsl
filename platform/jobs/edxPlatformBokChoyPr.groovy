package platform

import org.yaml.snakeyaml.Yaml
import static org.edx.jenkins.dsl.JenkinsPublicConstants.JENKINS_PUBLIC_LOG_ROTATOR
import static org.edx.jenkins.dsl.JenkinsPublicConstants.JENKINS_PUBLIC_JUNIT_REPORTS
import static org.edx.jenkins.dsl.JenkinsPublicConstants.GHPRB_BLACKLIST_BRANCH
import static org.edx.jenkins.dsl.JenkinsPublicConstants.GHPRB_WHITELIST_BRANCH
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
//                       blacklistBranchRegex: regular expression to filter which branches of a particular repo
//                       should not trigger builds (via GHRPB)
//                       context: Github context used to report test status
//                       triggerPhrase: Github comment used to trigger this job
//                       defaultTestengbranch: default branch of the testeng-ci repo for this job
//                       commentOnly: true/false if this job should only be triggered by explicit comments on
//                       github. Default behavior: triggered by comments AND pull request updates
//                       djangoVersion: version of django to run tests with (via tox)
//                       ]

Map publicJobConfig = [ open : true,
                        jobName : 'edx-platform-bok-choy-pr',
                        subsetJob: 'edx-platform-test-subset',
                        repoName: 'edx-platform',
                        workerLabel: 'jenkins-worker',
                        whitelistBranchRegex: /^((?!open-release\/).)*$/,
                        blacklistBranchRegex: 'estute/jenkins-ff-57-b',
                        context: 'jenkins/bokchoy',
                        triggerPhrase: /.*jenkins\W+run\W+bokchoy.*/,
                        defaultTestengBranch: 'master'
                        ]

Map django19JobConfig = [ open : true,
                          jobName : 'edx-platform-django-1.9-bok-choy-pr',
                          subsetJob: 'edx-platform-test-subset',
                          repoName: 'edx-platform',
                          workerLabel: 'django-upgrade-worker',
                          whitelistBranchRegex: /^((?!open-release\/).)*$/,
                          context: 'jenkins/django-1.9/bokchoy',
                          triggerPhrase: /.*jenkins\W+run\W+django19\W+bokchoy.*/,
                          defaultTestengBranch: 'master',
                          commentOnly: true,
                          djangoVersion: '1.9'
                          ]

Map django110JobConfig = [ open : true,
                           jobName : 'edx-platform-django-1.10-bok-choy-pr',
                           subsetJob: 'edx-platform-test-subset',
                           repoName: 'edx-platform',
                           workerLabel: 'django-upgrade-worker',
                           whitelistBranchRegex: /^((?!open-release\/).)*$/,
                           context: 'jenkins/django-1.10/bokchoy',
                           triggerPhrase: /.*jenkins\W+run\W+django110\W+bokchoy.*/,
                           defaultTestengBranch: 'master',
                           commentOnly: true,
                           djangoVersion: '1.10'
                           ]

Map django111JobConfig = [ open : true,
                           jobName : 'edx-platform-django-upgrade-bok-choy-pr',
                           subsetJob: 'edx-platform-test-subset',
                           repoName: 'edx-platform',
                           workerLabel: 'django-upgrade-worker',
                           whitelistBranchRegex: /^((?!open-release\/).)*$/,
                           context: 'jenkins/django-upgrade/bokchoy',
                           triggerPhrase: /.*jenkins\W+run\W+django\W+upgrade\W+bokchoy.*/,
                           defaultTestengBranch: 'master',
                           commentOnly: true,
                           djangoVersion: '1.11'
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

Map firefox57JobConfig = [ open : true,
                        jobName : 'edx-platform-firefox-upgrade-bok-choy-pr',
                        subsetJob: 'edx-platform-test-subset',
                        repoName: 'edx-platform',
                        workerLabel: 'ff-57-jenkins-worker',
                        whitelistBranchRegex: /estute\/jenkins-ff-57-b/,
                        context: 'jenkins/ff-57-bokchoy',
                        triggerPhrase: /.*jenkins\W+run\W+firefox\W+upgrade\W+bokchoy.*/,
                        defaultTestengBranch: 'master'
                        ]

List jobConfigs = [ publicJobConfig,
                    django19JobConfig,
                    django110JobConfig,
                    django111JobConfig,
                    privateJobConfig,
                    publicGinkgoJobConfig,
                    privateGinkgoJobConfig,
                    publicFicusJobConfig,
                    privateFicusJobConfig,
                    firefox57JobConfig
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
            // Only define the Django version if explicitly defined in a config.
            // Otherwise, the default version will be used
            if (jobConfig.containsKey('djangoVersion')) {
                env('DJANGO_VERSION', jobConfig.djangoVersion)
            }
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
            pullRequest {
                admins(ghprbMap['admin'])
                useGitHubHooks()
                triggerPhrase(jobConfig.triggerPhrase)
                if (jobConfig.commentOnly) {
                    onlyTriggerPhrase(true)
                }
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

        if (jobConfig.blacklistBranchRegex) {
            configure GHPRB_BLACKLIST_BRANCH(jobConfig.blacklistBranchRegex)
        }
        configure GHPRB_WHITELIST_BRANCH(jobConfig.whitelistBranchRegex)

        dslFile('testeng-ci/jenkins/flow/pr/edx-platform-bok-choy-pr.groovy')
        publishers { //publish JUnit Test report
            archiveJunit(JENKINS_PUBLIC_JUNIT_REPORTS)
            if (jobConfig.repoName == "edx-platform") {
                downstreamParameterized JENKINS_EDX_PLATFORM_TEST_NOTIFIER('${ghprbPullId}')
            }
        }
    }
}

package platform

import org.yaml.snakeyaml.Yaml
import static org.edx.jenkins.dsl.JenkinsPublicConstants.JENKINS_PUBLIC_LOG_ROTATOR
import static org.edx.jenkins.dsl.JenkinsPublicConstants.JENKINS_PUBLIC_JUNIT_REPORTS
import static org.edx.jenkins.dsl.JenkinsPublicConstants.GHPRB_WHITELIST_BRANCH
import static org.edx.jenkins.dsl.JenkinsPublicConstants.GENERAL_PRIVATE_JOB_SECURITY

/* stdout logger */
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
//                       runCoverage: whether or not the shards should run unit tests through coverage, and then
//                       run the coverage job on the results
//                       coverageJob: name of the coverage job to run after the unit tests
//                       workerLabel: label of the worker to run the subset jobs on
//                       whiteListBranchRegex: regular expression to filter which branches of a particular repo
//                       can will trigger builds (via GHRPB)
//                       context: Github context used to report test status
//                       triggerPhrase: Github comment used to trigger this job
//                       targetBranch: branch of the edx-platform used as a comparison when running coverage.
//                       this value is passed from the python job to the coverage job and used as an environment
//                       variable
//                       defaultTestengbranch: default branch of the testeng-ci repo for this job
//                       commentOnly: true/false if this job should only be triggered by explicit comments on
//                       github. Default behavior: triggered by comments AND pull request updates
//                       djangoVersion: version of django to run tests with (via tox)
//                       ]

// Individual Job Configurations
Map publicJobConfig = [ open: true,
                        jobName: 'edx-platform-python-unittests-pr',
                        subsetJob: 'edx-platform-test-subset',
                        repoName: 'edx-platform',
                        runCoverage: true,
                        coverageJob: 'edx-platform-unit-coverage',
                        workerLabel: 'jenkins-worker',
                        whitelistBranchRegex: /^((?!open-release\/).)*$/,
                        context: 'jenkins/python',
                        triggerPhrase: 'jenkins run python',
                        targetBranch: 'origin/master',
                        defaultTestengBranch: 'master'
                        ]

Map django19JobConfig = [ open: true,
                          jobName: 'edx-platform-django-1.9-unittests-pr',
                          subsetJob: 'edx-platform-test-subset',
                          repoName: 'edx-platform',
                          runCoverage: false,
                          coverageJob: 'edx-platform-unit-coverage',
                          workerLabel: 'django-upgrade-worker',
                          whitelistBranchRegex: /^((?!open-release\/).)*$/,
                          context: 'jenkins/django-1.9/python',
                          triggerPhrase: 'jenkins run django19 python',
                          targetBranch: 'origin/master',
                          defaultTestengBranch: 'master',
                          commentOnly: true,
                          djangoVersion: '1.9'
                          ]

Map django110JobConfig = [ open: true,
                           jobName: 'edx-platform-django-1.10-unittests-pr',
                           subsetJob: 'edx-platform-test-subset',
                           repoName: 'edx-platform',
                           runCoverage: false,
                           coverageJob: 'edx-platform-unit-coverage',
                           workerLabel: 'django-upgrade-worker',
                           whitelistBranchRegex: /^((?!open-release\/).)*$/,
                           context: 'jenkins/django-1.10/python',
                           triggerPhrase: 'jenkins run django110 python',
                           targetBranch: 'origin/master',
                           defaultTestengBranch: 'master',
                           commentOnly: true,
                           djangoVersion: '1.10'
                           ]

Map django111JobConfig = [ open: true,
                           jobName: 'edx-platform-django-upgrade-unittests-pr',
                           subsetJob: 'edx-platform-test-subset',
                           repoName: 'edx-platform',
                           runCoverage: false,
                           coverageJob: 'edx-platform-unit-coverage',
                           workerLabel: 'django-upgrade-worker',
                           whitelistBranchRegex: /^((?!open-release\/).)*$/,
                           context: 'jenkins/django-upgrade/python',
                           triggerPhrase: 'jenkins run django upgrade python',
                           targetBranch: 'origin/master',
                           defaultTestengBranch: 'master',
                           djangoVersion: '1.11'
                           ]

Map privateJobConfig = [ open: false,
                         jobName: 'edx-platform-python-unittests-pr_private',
                         subsetJob: 'edx-platform-test-subset_private',
                         repoName: 'edx-platform-private',
                         runCoverage: true,
                         coverageJob: 'edx-platform-unit-coverage_private',
                         workerLabel: 'jenkins-worker',
                         whitelistBranchRegex: /^((?!open-release\/).)*$/,
                         context: 'jenkins/python',
                         triggerPhrase: 'jenkins run python',
                         targetBranch: 'origin/security-release',
                         defaultTestengBranch: 'master'
                         ]

Map publicGinkgoJobConfig = [ open: true,
                              jobName: 'ginkgo-python-unittests-pr',
                              subsetJob: 'edx-platform-test-subset',
                              repoName: 'edx-platform',
                              runCoverage: true,
                              coverageJob: 'edx-platform-unit-coverage',
                              workerLabel: 'ginkgo-jenkins-worker',
                              whitelistBranchRegex: /open-release\/ginkgo.master/,
                              context: 'jenkins/ginkgo/python',
                              triggerPhrase: 'ginkgo run python',
                              targetBranch: 'origin/open-release/ginkgo.master',
                              defaultTestengBranch: 'origin/open-release/ginkgo.master'
                              ]

Map privateGinkgoJobConfig = [ open: false,
                               jobName: 'ginkgo-python-unittests-pr_private',
                               subsetJob: 'edx-platform-test-subset_private',
                               repoName: 'edx-platform-private',
                               runCoverage: true,
                               coverageJob: 'edx-platform-unit-coverage_private',
                               workerLabel: 'ginkgo-jenkins-worker',
                               whitelistBranchRegex: /open-release\/ginkgo.master/,
                               context: 'jenkins/ginkgo/python',
                               triggerPhrase: 'ginkgo run python',
                               targetBranch: 'origin/security/release',
                               defaultTestengBranch: 'origin/open-release/ginkgo.master'
                               ]

Map publicFicusJobConfig = [ open: true,
                             jobName: 'ficus-python-unittests-pr',
                             subsetJob: 'edx-platform-test-subset',
                             repoName: 'edx-platform',
                             runCoverage: true,
                             coverageJob: 'edx-platform-unit-coverage',
                             workerLabel: 'ficus-jenkins-worker',
                             whitelistBranchRegex: /open-release\/ficus.master/,
                             context: 'jenkins/ficus/python',
                             triggerPhrase: 'ficus run python',
                             targetBranch: 'origin/open-release/ficus.master',
                             defaultTestengBranch: 'origin/open-release/ficus.master'
                             ]

Map privateFicusJobConfig = [ open: false,
                              jobName: 'ficus-python-unittests-pr_private',
                              subsetJob: 'edx-platform-test-subset_private',
                              repoName: 'edx-platform-private',
                              runCoverage: true,
                              coverageJob: 'edx-platform-unit-coverage_private',
                              workerLabel: 'ficus-jenkins-worker',
                              whitelistBranchRegex: /open-release\/ficus.master/,
                              context: 'jenkins/ficus/python',
                              triggerPhrase: 'ficus run python',
                              targetBranch: 'origin/security-release',
                              defaultTestengBranch: 'origin/open-release/ficus.master'
                              ]

List jobConfigs = [ publicJobConfig,
                    django19JobConfig,
                    django110JobConfig,
                    django111JobConfig,
                    privateJobConfig,
                    publicGinkgoJobConfig,
                    privateGinkgoJobConfig,
                    publicFicusJobConfig,
                    privateFicusJobConfig
                    ]

// Iterate over the job configs to create individual build flow jobs
jobConfigs.each { jobConfig ->

    buildFlowJob(jobConfig.jobName) {

        if (!jobConfig.open.toBoolean()) {
            authorization GENERAL_PRIVATE_JOB_SECURITY()
        }
        properties {
            githubProjectUrl("https://github.com/edx/${jobConfig.repoName}/")
        }
        logRotator JENKINS_PUBLIC_LOG_ROTATOR(7)
        concurrentBuild()
        label('flow-worker-python')
        checkoutRetryCount(5)
        environmentVariables {
            env('SUBSET_JOB', jobConfig.subsetJob)
            env('REPO_NAME', jobConfig.repoName)
            env('RUN_COVERAGE', jobConfig.runCoverage)
            env('COVERAGE_JOB', jobConfig.coverageJob)
            env('TARGET_BRANCH', jobConfig.targetBranch)
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
           git {
                remote {
                    url("https://github.com/edx/${jobConfig.repoName}.git")
                    refspec('+refs/pull/*:refs/remotes/origin/pr/*')
                    if (!jobConfig['open'].toBoolean()) {
                        credentials("EDX_STATUS_BOT_CREDENTIALS")
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

        configure GHPRB_WHITELIST_BRANCH(jobConfig.whitelistBranchRegex)

        dslFile('testeng-ci/jenkins/flow/pr/edx-platform-python-unittests-pr.groovy')
        publishers {
            archiveJunit(JENKINS_PUBLIC_JUNIT_REPORTS) {
                retainLongStdout()
            }
            // Only archive Coverage data when explicitly defined in the jobConfig to avoid build errors
            if (jobConfig.runCoverage) {
                publishHtml {
                    report("${jobConfig.repoName}/reports") {
                        reportFiles('diff_coverage_combined.html')
                        reportName('Diff Coverage Report')
                        keepAll()
                    }
                }
                configure { node ->
                    node / publishers << 'jenkins.plugins.shiningpanda.publishers.CoveragePublisher' {
                    }
                }
            }
        }
    }
}

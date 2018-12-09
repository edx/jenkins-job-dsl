package platform

import org.yaml.snakeyaml.Yaml
import static org.edx.jenkins.dsl.JenkinsPublicConstants.JENKINS_PUBLIC_LOG_ROTATOR
import static org.edx.jenkins.dsl.JenkinsPublicConstants.JENKINS_PUBLIC_GITHUB_BASEURL
import static org.edx.jenkins.dsl.JenkinsPublicConstants.GHPRB_WHITELIST_BRANCH

/*
Example secret YAML file used by this script
publicJobConfig:
    open : true/false
    jobName : name-of-jenkins-job-to-be
    repoName : name-of-github-edx-repo
    protocol : protocol-and-base-url
    platformUrl : platform-github-url-segment.git
    platformCredential : n/a
    platformCloneReference : clone/.git
    workerLabel: worker-label
    whitelistBranchRegex: 'release/*'
    context: jenkins/test
    triggerPhrase: 'jenkins run test'
*/

/* stdout logger */
/* use this instead of println, because you can pass it into closures or other scripts. */
Map config = [:]
Binding bindings = getBinding()
config.putAll(bindings.getVariables())
PrintStream out = config['out']

/* Map to hold the k:v pairs parsed from the secret file */
Map secretMap = [:]
Map ghprbMap = [:]
try {
    out.println('Parsing secret YAML file')
    /* Parse k:v pairs from the secret file referenced by secretFileVariable */
    String secretFileContents = new File("${EDX_PLATFORM_QUALITY_PR_SECRET}").text
    String ghprbConfigContents = new File("${GHPRB_SECRET}").text
    Yaml yaml = new Yaml()
    secretMap = yaml.load(secretFileContents)
    ghprbMap = yaml.load(ghprbConfigContents)
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
    assert jobConfig.containsKey('repoName')
    assert jobConfig.containsKey('platformUrl')
    assert jobConfig.containsKey('platformCredential')
    assert jobConfig.containsKey('platformCloneReference')
    assert jobConfig.containsKey('protocol')
    assert jobConfig.containsKey('workerLabel')
    assert jobConfig.containsKey('whitelistBranchRegex')
    assert jobConfig.containsKey('context')
    assert jobConfig.containsKey('triggerPhrase')
    assert ghprbMap.containsKey('admin')
    assert ghprbMap.containsKey('userWhiteList')
    assert ghprbMap.containsKey('orgWhiteList')

    job(jobConfig['jobName']) {

        /* For non-open jobs, enable project based security */
        if (!jobConfig['open'].toBoolean()) {
            authorization {
                blocksInheritance(true)
                permissionAll('edx')
                permission('hudson.model.Item.Discover', 'anonymous')
            }
        }
        description('This job runs pull requests through our quality checks.')
        logRotator JENKINS_PUBLIC_LOG_ROTATOR()
        properties {
              githubProjectUrl(JENKINS_PUBLIC_GITHUB_BASEURL + jobConfig['platformUrl'])
        }
        concurrentBuild()
        parameters {
            labelParam('WORKER_LABEL') {
                description('Select a Jenkins worker label for running this job')
                defaultValue(jobConfig['workerLabel'])
            }
        }
        scm {
            git { //using git on the branch and url, clone, clean before checkout
                remote {
                    url(jobConfig['protocol'] + jobConfig['platformUrl'] + '.git')
                    refspec('+refs/pull/*:refs/remotes/origin/pr/*')
                    if (!jobConfig['open'].toBoolean()) {
                        credentials(jobConfig['platformCredential'])
                    }
                }
                branch('\${ghprbActualCommit}')
                browser()
                extensions {
                    cloneOptions {
                        reference("\$HOME/" + jobConfig['platformCloneReference'])
                        timeout(10)
                    }
                    cleanBeforeCheckout()
                    relativeTargetDirectory(jobConfig['repoName'])
                }
            }
        }
        triggers { //trigger when pull request is created
            pullRequest {
                admins(ghprbMap['admin'])
                useGitHubHooks()
                triggerPhrase(jobConfig['triggerPhrase'])
                userWhitelist(ghprbMap['userWhiteList'])
                orgWhitelist(ghprbMap['orgWhiteList'])
                extensions {
                    commitStatus {
                        context(jobConfig['context'])
                    }
                }
             }
        }

        configure GHPRB_WHITELIST_BRANCH(jobConfig['whitelistBranchRegex'])

        wrappers {
            timeout {
                absolute(90)
            }
            timestamps()
            colorizeOutput()
            if (!jobConfig['open'].toBoolean()) {
                sshAgent(jobConfig['platformCredential'])
            }
            buildName('#\${BUILD_NUMBER}: Quality Tests')
        }
        steps {
            shell("cd ${jobConfig['repoName']}; TEST_SUITE=quality ./scripts/all-tests.sh")
        }
        publishers {
            archiveArtifacts {
                pattern('edx-platform*/reports/**/*,edx-platform*/test_root/log/*.png,edx-platform*/' +
                        'test_root/log/*.log,edx-platform*/test_root/log/hars/*.har,edx-platform*/**/' +
                        'nosetests.xml,edx-platform*/**/TEST-*.xml')
                defaultExcludes(true)
                allowEmpty(true)
            }
            publishHtml {
                report("${jobConfig['repoName']}/reports/metrics/") {
                    reportName('Quality Report')
                    reportFiles('pylint/*view*/,pep8/*view*/,jshint/*view*/,python_complexity/*view*/,' +
                                'xsscommitlint/*view*/,xsslint/*view*/,eslint/*view*/')
                    keepAll(true)
                    allowMissing(true)
                }
                report("${jobConfig['repoName']}/reports/diff_quality") {
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

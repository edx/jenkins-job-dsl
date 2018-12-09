package devops

import org.yaml.snakeyaml.Yaml
import static org.edx.jenkins.dsl.JenkinsPublicConstants.JENKINS_PUBLIC_LOG_ROTATOR
import static org.edx.jenkins.dsl.JenkinsPublicConstants.JENKINS_PUBLIC_JUNIT_REPORTS
import static org.edx.jenkins.dsl.JenkinsPublicConstants.JENKINS_PUBLIC_GITHUB_BASEURL
import static org.edx.jenkins.dsl.JenkinsPublicConstants.GHPRB_WHITELIST_BRANCH

/*
Example secret YAML file used by this script
publicJobConfig:
    open : true/false
    jobName : name-of-jenkins-job-to-be
    protocol : protocol-and-base-url
    url : github-url-segment
    repoName : name-of-github-edx-repo
    credential : n/a
    cloneReference : clone/.git
    workerLabel: worker-label
    whitelistBranchRegex: 'release/*'
    context: jenkins/test
    triggerPhrase: 'jenkins run test' */

String archiveReports = 'edx-platform*/reports/**/*,edx-platform*/test_root/log/*.png,'
archiveReports += 'edx-platform*/test_root/log/*.log,edx-platform*/test_root/log/hars/*.har,'
archiveReports += 'edx-platform*/**/nosetests.xml,edx-platform*/**/TEST-*.xml'

String descriptionString = 'This job runs pull requests through our javascript tests.<br><br> \n'
descriptionString += 'To run failed tests on devstack, see test patterns <a href=https://github.'
descriptionString += 'com/edx/edx-platform/blob/master/docs/en_us/internal/testing.rst>here</a>'

/* stdout logger */
Map config = [:]
Binding bindings = getBinding()
config.putAll(bindings.getVariables())
PrintStream out = config['out']

/* Map to hold the k:v pairs parsed from the secret file */
Map secretMap = [:]
Map ghprbMap = [:]
try {
    out.println('Parsing secret YAML file')
    String secretFileContents = new File("${EDX_PLATFORM_TEST_JS_PR_SECRET}").text
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

    assert jobConfig.containsKey('open')
    assert jobConfig.containsKey('jobName')
    assert jobConfig.containsKey('protocol')
    assert jobConfig.containsKey('url')
    assert jobConfig.containsKey('repoName')
    assert jobConfig.containsKey('credential')
    assert jobConfig.containsKey('cloneReference')
    assert jobConfig.containsKey('workerLabel')
    assert jobConfig.containsKey('whitelistBranchRegex')
    assert jobConfig.containsKey('context')
    assert jobConfig.containsKey('triggerPhrase')
    assert ghprbMap.containsKey('admin')
    assert ghprbMap.containsKey('userWhiteList')
    assert ghprbMap.containsKey('orgWhiteList')

    job(jobConfig['jobName']) {
        description(descriptionString)
        if (!jobConfig['open'].toBoolean()) {
            authorization {
                blocksInheritance(true)
                permissionAll('edx')
                permission('hudson.model.Item.Discover', 'anonymous')
            }
        }
        properties {
            githubProjectUrl(JENKINS_PUBLIC_GITHUB_BASEURL + jobConfig['url'])
        }
        logRotator JENKINS_PUBLIC_LOG_ROTATOR()
        concurrentBuild()
        parameters {
            labelParam('WORKER_LABEL') {
                description('Select a Jenkins worker label for running this job')
                defaultValue(jobConfig['workerLabel'])
            }
        }
        scm {
            git {
                remote {
                    url(jobConfig['protocol'] + jobConfig['url'] + '.git')
                    refspec('+refs/pull/*:refs/remotes/origin/pr/*')
                    if (!jobConfig['open'].toBoolean()) {
                        credentials(jobConfig['credential'])
                    }
                }
                branch('\${ghprbActualCommit}')
                browser()
                extensions {
                    cloneOptions {
                        reference("\$HOME/" + jobConfig['cloneReference'])
                        timeout(10)
                    }
                    relativeTargetDirectory(jobConfig['repoName'])
                }
            }
        }
        triggers {
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
               absolute(45)
           }
           timestamps()
           colorizeOutput()
           if (!jobConfig['open'].toBoolean()) {
                sshAgent(jobConfig['credential'])
           }
           buildName('#${BUILD_NUMBER}: Javascript Tests')
       }
       steps {
           shell("cd ${jobConfig['repoName']}; TEST_SUITE=js-unit ./scripts/all-tests.sh")
       }
       publishers {
           archiveArtifacts {
               pattern(archiveReports)
               defaultExcludes()
           }
           cobertura ('edx-platform*/**/reports/**/coverage*.xml') {
               failNoReports(true)
               sourceEncoding('ASCII')
               methodTarget(80, 0, 0)
               lineTarget(80, 0, 0)
               conditionalTarget(70, 0, 0)
           }
           publishHtml {
               report('edx-platform*/reports') {
                   reportFiles('diff_coverage_combined.html')
                   reportName('Diff Coverage Report')
                   keepAll()
                   allowMissing()
               }
           }
           archiveJunit(JENKINS_PUBLIC_JUNIT_REPORTS)
       }
    }
}

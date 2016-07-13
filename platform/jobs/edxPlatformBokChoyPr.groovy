package devops

import hudson.model.Build
import static org.edx.jenkins.dsl.JenkinsPublicConstants.JENKINS_PUBLIC_LOG_ROTATOR
import static org.edx.jenkins.dsl.JenkinsPublicConstants.JENKINS_PUBLIC_PARSE_SECRET
import static org.edx.jenkins.dsl.JenkinsPublicConstants.JENKINS_PUBLIC_JUNIT_REPORTS
import static org.edx.jenkins.dsl.JenkinsPublicConstants.JENKINS_PUBLIC_GITHUB_BASEURL
import org.yaml.snakeyaml.Yaml

/*
Example secret YAML file used by this script
publicJobConfig:
    open : true/false
    jobName : name-of-jenkins-job-to-be
    subsetJob : name-of-test-subset-job
    repoName : name-of-github-edx-repo
    testengUrl: testeng-github-url-segment.git
    platformUrl : platform-github-url-segment.git
    testengCredential : n/a
    platformCredential : n/a
    platformCloneReference : clone/.git
    admin : [name, name, name]
    userWhiteList : [name, name, name]
    orgWhiteList : [name, name, name]
*/

/* stdout logger */
/* use this instead of println, because you can pass it into closures or other scripts. */
/* TODO: Move this into JenkinsPublicConstants, as it can be shared. */
Map config = [:]
Binding bindings = getBinding()
config.putAll(bindings.getVariables())
PrintStream out = config['out']

/* Environment variable (set in Seeder job config) to reference a Jenkins secret file */
String secretFileVariable = 'EDX_PLATFORM_TEST_BOK_CHOY_PR_SECRET'

/* Map to hold the k:v pairs parsed from the secret file */
Map secretMap = [:]
try {
    out.println('Parsing secret YAML file')
    /* Parse k:v pairs from the secret file referenced by secretFileVariable */
    String contents = new File("${EDX_PLATFORM_TEST_BOK_CHOY_PR_SECRET}").text
    Yaml yaml = new Yaml()
    secretMap = yaml.load(contents)
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
    /* TODO: Use/Build a more robust test framework for this */
    assert jobConfig.containsKey('open')
    assert jobConfig.containsKey('jobName')
    assert jobConfig.containsKey('subsetJob')
    assert jobConfig.containsKey('repoName')
    assert jobConfig.containsKey('testengUrl')
    assert jobConfig.containsKey('platformUrl')
    assert jobConfig.containsKey('testengCredential')
    assert jobConfig.containsKey('platformCredential')
    assert jobConfig.containsKey('platformCloneReference')
    assert jobConfig.containsKey('admin')
    assert jobConfig.containsKey('userWhiteList')
    assert jobConfig.containsKey('orgWhiteList')

    buildFlowJob(jobConfig['jobName']) {

        /* For non-open jobs, enable project based security */
        if (!jobConfig['open'].toBoolean()) {
            authorization {
                blocksInheritance(true)
                permissionAll('edx')
            }
        }
        properties {
              githubProjectUrl(JENKINS_PUBLIC_GITHUB_BASEURL + jobConfig['platformUrl'])
        }
        logRotator JENKINS_PUBLIC_LOG_ROTATOR() //Discard build after a certain amount of time
        concurrentBuild() //concurrent builds can happen
        label('flow-worker-bokchoy') //restrict to jenkins-worker
        checkoutRetryCount(5)
        environmentVariables {
            env("SUBSET_JOB", jobConfig['subsetJob'])
            env("REPO_NAME", jobConfig['repoName'])
        }
        multiscm {
            git { //using git on the branch and url, clean before checkout
                remote {
                    url(JENKINS_PUBLIC_GITHUB_BASEURL + jobConfig['testengUrl'] + '.git')
                    if (!jobConfig['open'].toBoolean()) {
                        credentials(jobConfig['testengCredential'])
                    }
                }
                branch('*/master')
                browser()
                extensions {
                    relativeTargetDirectory('testeng-ci')
                    cleanBeforeCheckout()
                }
            }
            git { //using git on the branch and url, clone, clean before checkout
                remote {
                    url(JENKINS_PUBLIC_GITHUB_BASEURL + jobConfig['platformUrl'] + '.git')
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
                admins(jobConfig['admin'])
                useGitHubHooks()
                triggerPhrase('jenkins run bokchoy')
                userWhitelist(jobConfig['userWhiteList'])
                orgWhitelist(jobConfig['orgWhiteList'])
                extensions {
                    commitStatus {
                        context('jenkins/bokchoy')
                    }
                }
            }
        }
        dslFile('testeng-ci/jenkins/flow/pr/edx-platform-bok-choy-pr.groovy')
        publishers { //publish JUnit Test report
            archiveJunit(JENKINS_PUBLIC_JUNIT_REPORTS)
        }
    }
}

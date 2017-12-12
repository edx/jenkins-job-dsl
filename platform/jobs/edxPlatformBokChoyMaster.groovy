package devops

import org.yaml.snakeyaml.Yaml
import static org.edx.jenkins.dsl.JenkinsPublicConstants.JENKINS_PUBLIC_LOG_ROTATOR
import static org.edx.jenkins.dsl.JenkinsPublicConstants.JENKINS_PUBLIC_HIPCHAT
import static org.edx.jenkins.dsl.JenkinsPublicConstants.JENKINS_PUBLIC_BASE_URL
import static org.edx.jenkins.dsl.JenkinsPublicConstants.JENKINS_PUBLIC_JUNIT_REPORTS
import static org.edx.jenkins.dsl.JenkinsPublicConstants.JENKINS_PUBLIC_GITHUB_STATUS_SUCCESS
import static org.edx.jenkins.dsl.JenkinsPublicConstants.JENKINS_PUBLIC_GITHUB_STATUS_UNSTABLE_OR_WORSE
import static org.edx.jenkins.dsl.JenkinsPublicConstants.JENKINS_PUBLIC_GITHUB_BASEURL

/*
Example secret YAML file used by this script
publicJobConfig:
    open : true/false
    jobName : name-of-jenkins-job-to-be
    subsetJob : name-of-test-subset-job
    repoName : name-of-github-edx-repo
    testengUrl: testeng-github-url-segment
    platformUrl : platform-github-url-segment
    testengCredential : n/a
    platformCredential : n/a
    platformCloneReference : clone/.git
    email : email-address@email.com
    hipchat : token
    workerLabel : worker-label
    refSpec : '+refs/heads/master:refs/remotes/origin/master'
    context : 'jenkins/test'
    defaultBranch : 'master'
    defaultTestengBranch: 'master'
    disabled: true/false
*/

/* stdout logger */
/* use this instead of println, because you can pass it into closures or other scripts. */
Map config = [:]
Binding bindings = getBinding()
config.putAll(bindings.getVariables())
PrintStream out = config['out']

/* Map to hold the k:v pairs parsed from the secret file */
Map secretMap = [:]
try {
    out.println('Parsing secret YAML file')
    /* Parse k:v pairs from the secret file referenced by secretFileVariable */
    String contents = new File("${EDX_PLATFORM_TEST_BOK_CHOY_SECRET}").text
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
    assert jobConfig.containsKey('repoName')
    assert jobConfig.containsKey('testengUrl')
    assert jobConfig.containsKey('testengUrl')
    assert jobConfig.containsKey('platformUrl')
    assert jobConfig.containsKey('testengCredential')
    assert jobConfig.containsKey('platformCredential')
    assert jobConfig.containsKey('platformCloneReference')
    assert jobConfig.containsKey('hipchat')
    assert jobConfig.containsKey('email')
    assert jobConfig.containsKey('workerLabel')
    assert jobConfig.containsKey('refSpec')
    assert jobConfig.containsKey('context')
    assert jobConfig.containsKey('defaultBranch')
    assert jobConfig.containsKey('defaultTestengBranch')
    assert jobConfig.containsKey('disabled')

    buildFlowJob(jobConfig['jobName']) {

        // automatically disable certain jobs for branches that don't always exist
        // to avoid incessant polling
        if (jobConfig['disabled'].toBoolean()) {
            disabled()
            description('This job is disabled by default, as the target platform' +
                        'branch is not guaranteed to always exist. If you need to' +
                        'run this job, make sure you manually enable it, and ' +
                        'disable it when you are finished')
        }

        /* For non-open jobs, enable project based security */
        if (!jobConfig['open'].toBoolean()) {
            authorization {
                blocksInheritance(true)
                permissionAll('edx')
                permission('hudson.model.Item.Discover', 'anonymous')
            }
        }

        parameters {
            stringParam('ENV_VARS', '', '')
            stringParam('WORKER_LABEL', jobConfig['workerLabel'], 'Jenkins worker for running the test subset jobs')
        }

        properties {
              githubProjectUrl(JENKINS_PUBLIC_GITHUB_BASEURL + jobConfig['platformUrl'])
        }
        logRotator JENKINS_PUBLIC_LOG_ROTATOR(7)
        concurrentBuild() //concurrent builds can happen
        label('flow-worker-bokchoy') //restrict to flow-worker-bokchoy
        checkoutRetryCount(5)
        environmentVariables {
            env('SUBSET_JOB', jobConfig['subsetJob'])
            env('REPO_NAME', jobConfig['repoName'])
        }
        multiscm {
           git { //using git on the branch and url, clone, clean before checkout
                remote {
                    url(JENKINS_PUBLIC_GITHUB_BASEURL + jobConfig['platformUrl'] + '.git')
                    refspec(jobConfig['refSpec'])
                    if (!jobConfig['open'].toBoolean()) {
                        credentials(jobConfig['platformCredential'])
                    }
                }
                branch(jobConfig['defaultBranch'])
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
            git { //using git on the branch and url, clean before checkout
                remote {
                    url(JENKINS_PUBLIC_GITHUB_BASEURL + jobConfig['testengUrl'] + '.git')
                    if (!jobConfig['open'].toBoolean()) {
                        credentials(jobConfig['testengCredential'])
                    }
                }
                branch(jobConfig['defaultTestengBranch'])
                browser()
                extensions {
                    cleanBeforeCheckout()
                    relativeTargetDirectory('testeng-ci')
                }
            }
        }
        triggers {
            // Trigger jobs via github pushes
            githubPush()
        }

        wrappers {
            timestamps()
        }

        Map <String, String> predefinedPropsMap  = [:]
        predefinedPropsMap.put('GIT_SHA', '${GIT_COMMIT}')
        predefinedPropsMap.put('GITHUB_ORG', 'edx')
        predefinedPropsMap.put('CONTEXT', jobConfig['context'])
        predefinedPropsMap.put('GITHUB_REPO', jobConfig['repoName'])
        predefinedPropsMap.put('TARGET_URL', JENKINS_PUBLIC_BASE_URL +
                                  'job/' + jobConfig['jobName'] + '/${BUILD_NUMBER}/')
        dslFile('testeng-ci/jenkins/flow/master/edx-platform-bok-choy-master.groovy')
        publishers { //JUnit Test report, trigger GitHub-Build-Status, email, message hipchat
            archiveJunit(JENKINS_PUBLIC_JUNIT_REPORTS)
            downstreamParameterized JENKINS_PUBLIC_GITHUB_STATUS_SUCCESS.call(predefinedPropsMap)
            downstreamParameterized JENKINS_PUBLIC_GITHUB_STATUS_UNSTABLE_OR_WORSE.call(predefinedPropsMap)
            mailer(jobConfig['email'])
            hipChat JENKINS_PUBLIC_HIPCHAT.call(jobConfig['hipchat'])
       }
    }
}

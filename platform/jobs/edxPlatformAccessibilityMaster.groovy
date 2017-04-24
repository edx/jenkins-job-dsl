package devops

import org.yaml.snakeyaml.Yaml
import static org.edx.jenkins.dsl.JenkinsPublicConstants.JENKINS_PUBLIC_LOG_ROTATOR
import static org.edx.jenkins.dsl.JenkinsPublicConstants.JENKINS_PUBLIC_HIPCHAT
import static org.edx.jenkins.dsl.JenkinsPublicConstants.JENKINS_PUBLIC_BASE_URL
import static org.edx.jenkins.dsl.JenkinsPublicConstants.JENKINS_PUBLIC_JUNIT_REPORTS
import static org.edx.jenkins.dsl.JenkinsPublicConstants.JENKINS_PUBLIC_GITHUB_STATUS_PENDING
import static org.edx.jenkins.dsl.JenkinsPublicConstants.JENKINS_PUBLIC_GITHUB_STATUS_SUCCESS
import static org.edx.jenkins.dsl.JenkinsPublicConstants.JENKINS_PUBLIC_GITHUB_STATUS_UNSTABLE_OR_WORSE
import static org.edx.jenkins.dsl.JenkinsPublicConstants.JENKINS_PUBLIC_GITHUB_BASEURL

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
    hipchat : token
    workerLabel : worker-label
    refSpec : '+refs/heads/master:refs/remotes/origin/master'
    context : 'jenkins/test'
    defaultBranch : 'master'
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
    String contents = new File("${EDX_PLATFORM_TEST_ACCESSIBILITY_SECRET}").text
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
    assert jobConfig.containsKey('url')
    assert jobConfig.containsKey('repoName')
    assert jobConfig.containsKey('protocol')
    assert jobConfig.containsKey('credential')
    assert jobConfig.containsKey('cloneReference')
    assert jobConfig.containsKey('hipchat')
    assert jobConfig.containsKey('email')
    assert jobConfig.containsKey('workerLabel')
    assert jobConfig.containsKey('refSpec')
    assert jobConfig.containsKey('context')
    assert jobConfig.containsKey('defaultBranch')
    assert jobConfig.containsKey('disabled')

    job(jobConfig['jobName']) {

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
            }
        }
        properties {
            githubProjectUrl(JENKINS_PUBLIC_GITHUB_BASEURL + jobConfig['url'])
        }
        parameters {
            labelParam('WORKER_LABEL') {
                description('Select a Jenkins worker label for running this job')
                defaultValue(jobConfig['workerLabel'])
            }
        }
        logRotator JENKINS_PUBLIC_LOG_ROTATOR() //discard old builds after 14 days
        concurrentBuild() //concurrent builds can happen
        scm {
            git { //using git on the branch and url, clone, clean before checkout
                remote {
                    url(jobConfig['protocol'] + jobConfig['url'] + '.git')
                    refspec(jobConfig['refSpec'])
                    if (!jobConfig['open'].toBoolean()) {
                        credentials(jobConfig['credential'])
                    }
                }
                branch(jobConfig['defaultBranch'])
                browser()
                extensions {
                    cloneOptions {
                        reference('\$HOME/' + jobConfig['cloneReference'])
                        timeout(10)
                    }
                    cleanBeforeCheckout()
                    relativeTargetDirectory(jobConfig['repoName'])
                }
            }
        }
        triggers {
            // due to a bug or misconfiguration, jobs with default branches with
            // slashes are indiscriminately triggered by pushes to other branches.
            // For more information, see:
            // https://openedx.atlassian.net/browse/TE-1921
            // for commits merging into master, trigger jobs via github pushes
            if ( jobConfig['defaultBranch'] == 'master') {
                githubPush()
            }
            // for all other jobs in this style, poll github for new commits on
            // the 'defaultBranch'
            else {
                scm("@hourly")
            }
        }
        wrappers { //abort when stuck after 75 minutes, use gnome-terminal coloring, have timestamps at Console
            timeout {
                absolute(75)
            }
            timestamps()
            colorizeOutput('gnome-terminal')
            if (!jobConfig['open'].toBoolean()) {
                sshAgent(jobConfig['credential'])
            }
        }

        Map <String, String> predefinedPropsMap  = [:]
        predefinedPropsMap.put('GIT_SHA', '${GIT_COMMIT}')
        predefinedPropsMap.put('GITHUB_ORG', 'edx')
        predefinedPropsMap.put('CONTEXT', jobConfig['context'])
        predefinedPropsMap.put('GITHUB_REPO', jobConfig['repoName'])
        predefinedPropsMap.put('TARGET_URL', JENKINS_PUBLIC_BASE_URL +
                                  'job/' + jobConfig['jobName'] + '/${BUILD_NUMBER}/')
        steps { //trigger GitHub-Build-Status and run accessibility tests
            downstreamParameterized JENKINS_PUBLIC_GITHUB_STATUS_PENDING.call(predefinedPropsMap)
            shell("cd ${jobConfig['repoName']}; RUN_PA11YCRAWLER=0 ./scripts/accessibility-tests.sh")
        }
        publishers { //publish artifacts and JUnit Test report, trigger GitHub-Build-Status, message on hipchat
           archiveArtifacts {
               pattern(JENKINS_PUBLIC_JUNIT_REPORTS)
               pattern('edx-platform*/test_root/log/**/*.png')
               pattern('edx-platform*/test_root/log/**/*.log')
               pattern('edx-platform*/reports/pa11ycrawler/**/*')
               allowEmpty()
               defaultExcludes()
           }
           publishHtml {
               report(jobConfig['repoName'] + '/reports/pa11ycrawler/html') {
               reportName('HTML Report')
               allowMissing()
               keepAll()
               }
           }
           archiveJunit(JENKINS_PUBLIC_JUNIT_REPORTS)
           downstreamParameterized JENKINS_PUBLIC_GITHUB_STATUS_SUCCESS.call(predefinedPropsMap)
           downstreamParameterized JENKINS_PUBLIC_GITHUB_STATUS_UNSTABLE_OR_WORSE.call(predefinedPropsMap)
           mailer(jobConfig['email'])
           hipChat JENKINS_PUBLIC_HIPCHAT.call(jobConfig['hipchat'])
       }
    }
}

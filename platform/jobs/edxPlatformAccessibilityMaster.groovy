package devops

import hudson.model.Build
import static org.edx.jenkins.dsl.JenkinsPublicConstants.JENKINS_PUBLIC_LOG_ROTATOR
import static org.edx.jenkins.dsl.JenkinsPublicConstants.JENKINS_PUBLIC_WORKER
import static org.edx.jenkins.dsl.JenkinsPublicConstants.JENKINS_PUBLIC_PARSE_SECRET
import static org.edx.jenkins.dsl.JenkinsPublicConstants.JENKINS_PUBLIC_HIPCHAT
import static org.edx.jenkins.dsl.JenkinsPublicConstants.JENKINS_PUBLIC_BASE_URL
import static org.edx.jenkins.dsl.JenkinsPublicConstants.JENKINS_PUBLIC_JUNIT_REPORTS
import static org.edx.jenkins.dsl.JenkinsPublicConstants.JENKINS_PUBLIC_GITHUB_STATUS_PENDING
import static org.edx.jenkins.dsl.JenkinsPublicConstants.JENKINS_PUBLIC_GITHUB_STATUS_SUCCESS
import static org.edx.jenkins.dsl.JenkinsPublicConstants.JENKINS_PUBLIC_GITHUB_STATUS_UNSTABLE_OR_WORSE

/*
Example secret YAML file used by this script
publicJobConfig:
    open : true/false
    jobName : name-of-jenkins-job-to-be
    url : github-url-segment
    credential : n/a
    cloneReference : clone/.git
    hipchat : token
*/

Map <String, String> predefinedPropsMap  = [:]
predefinedPropsMap.put('GIT_SHA', '${GIT_COMMIT}')
predefinedPropsMap.put('GITHUB_ORG', 'edx')
predefinedPropsMap.put('GITHUB_REPO', 'edx-platform')
predefinedPropsMap.put('TARGET_URL', JENKINS_PUBLIC_BASE_URL +
    'view/accessibility/job/edx-platform-accessibility-master/${BUILD_NUMBER}/')
predefinedPropsMap.put('CONTEXT', 'jenkins/a11y')

/* stdout logger */
/* use this instead of println, because you can pass it into closures or other scripts. */
/* TODO: Move this into JenkinsPublicConstants, as it can be shared. */
Map config = [:]
Binding bindings = getBinding()
config.putAll(bindings.getVariables())
PrintStream out = config['out']

params = [
    name: 'sha1',
    description: 'Sha1 hash of branch to build. Default branch : master',
    default: 'refs/heads/master' ]

/* Environment variable (set in Seeder job config) to reference a Jenkins secret file */
String secretFileVariable = 'EDX_PLATFORM_TEST_ACCESSIBILITY_SECRET'

/* Map to hold the k:v pairs parsed from the secret file */
Map secretMap = [:]
try {
    out.println('Parsing secret YAML file')
    /* Parse k:v pairs from the secret file referenced by secretFileVariable */
    Thread thread = Thread.currentThread()
    Build build = thread?.executable
    Map envVarsMap = build.parent.builds[0].properties.get("envVars")
    secretMap = JENKINS_PUBLIC_PARSE_SECRET.call(secretFileVariable, envVarsMap, out)
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
    assert jobConfig.containsKey('credential')
    assert jobConfig.containsKey('cloneReference')
    assert jobConfig.containsKey('hipchat')

    job(jobConfig['jobName']) {

        /* For non-open jobs, enable project based security */
        if (!jobConfig['open'].toBoolean()) {
            authorization {
                blocksInheritance(true)
                permissionAll('edx')
            }
        }

        parameters {
            stringParam(params.name, params.default, params.description)
        }
        logRotator JENKINS_PUBLIC_LOG_ROTATOR() //discard old builds after 14 days
        concurrentBuild() //concurrent builds can happen
        label(JENKINS_PUBLIC_WORKER) //restrict to jenkins-worker
        scm {
            git { //using git on the branch and url, clone, clean before checkout
                remote {
                    github(jobConfig['url'])
                    refspec('+refs/heads/master:refs/remotes/origin/master')
                    if (!jobConfig['open'].toBoolean()) {
                        credentials(jobConfig['credential'])
                    }
                }
                branch('\${sha1}')
                browser()
                extensions {
                    cloneOptions {
                        reference('\$HOME/' + jobConfig['cloneReference'])
                        timeout(10)
                    }
                    cleanBeforeCheckout()
                    relativeTargetDirectory('edx-platform')
                }
            }
        }
        triggers { //trigger when change pushed to GitHub
            githubPush()
        }
        wrappers { //abort when stuck after 75 minutes, use gnome-terminal coloring, have timestamps at Console
            timeout {
               absolute(75)
           }
           timestamps()
           colorizeOutput('gnome-terminal')
       }
       steps { //trigger GitHub-Build-Status and run accessibility tests
           downstreamParameterized JENKINS_PUBLIC_GITHUB_STATUS_PENDING.call(predefinedPropsMap) 
           shell('cd edx-platform; RUN_PA11YCRAWLER=1 ./scripts/accessibility-tests.sh')
       }
       publishers { //publish artifacts and JUnit Test report, trigger GitHub-Build-Status, message on hipchat
           archiveArtifacts {
               pattern(JENKINS_PUBLIC_JUNIT_REPORTS)
               pattern('edx-platform/test_root/log/**/*.png')
               pattern('edx-platform/test_root/log/**/*.log')
               pattern('edx-platform/reports/pa11ycrawler/**/*')
               allowEmpty()
               defaultExcludes()
           }
           archiveJunit(JENKINS_PUBLIC_JUNIT_REPORTS)
           downstreamParameterized JENKINS_PUBLIC_GITHUB_STATUS_SUCCESS.call(predefinedPropsMap)
           downstreamParameterized JENKINS_PUBLIC_GITHUB_STATUS_UNSTABLE_OR_WORSE.call(predefinedPropsMap)
           hipChat JENKINS_PUBLIC_HIPCHAT.call(jobConfig['hipchat'])
       }
    }
}

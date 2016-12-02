package devops

import org.yaml.snakeyaml.Yaml
import static org.edx.jenkins.dsl.JenkinsPublicConstants.JENKINS_PUBLIC_LOG_ROTATOR
import static org.edx.jenkins.dsl.JenkinsPublicConstants.JENKINS_PUBLIC_GITHUB_BASEURL

/*
This script uses two secret files
1 -  For general configuration of script

    authUser : name-of-auth-user-for-stage
    authPass : password-of-auth-user-for-stage
    loginEmail : email-for-stage-login
    loginPass : password-for-stage-login
    open : true/false
    email: email_for_notification@email.com
    githubRepoUrlSegment : url segment of repo i.e edx/repo_name
    jobName : job-name-to-be-created
    platformCredential: n/a

2 - For setting up github admin and whitelisting users/orgs for the pull request

    admin: admin(s) for the repo set as a list
    userWhiteList: user(s) to whitelist set as a list
    orgWhiteList: organization(s) to whitelist set as a list

*/

/* stdout logger */
/* use this instead of println, because you can pass it into closures or other scripts. */
Map config = [:]
Binding bindings = getBinding()
config.putAll(bindings.getVariables())
PrintStream out = config['out']

stringParams = [
    [
    name: 'COURSE_ORG',
    description: 'Organization name of the course',
    default: 'ArbiRaees'
    ],
    [
    name: 'COURSE_NUMBER',
    description: 'Course number',
    default: 'AR-1000'
    ],
    [
    name: 'COURSE_RUN',
    description: 'Term in which course will run',
    default: 'fall'
    ],
    [
    name: 'COURSE_DISPLAY_NAME',
    description: 'Display name of the course',
    default: 'Manual Smoke Test Course 1 - Auto'
    ]
]

/* Map to hold the k:v pairs parsed from the secret files */
Map secretMap = [:]
Map ghprbMap = [:]
out.println('Parsing secret YAML file')
try {
    /* Parse k:v pairs from the secret files referenced by secretFileContents and ghprbConfigContents */
    String secretFileContents = new File("${EDX_END_TO_END_TESTS}").text
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

    /* Test secrets contain all necessary keys for this job */
    /* TODO: Use/Build a more robust test framework for this */
    assert jobConfig.containsKey('authUser')
    assert jobConfig.containsKey('authPass')
    assert jobConfig.containsKey('loginEmail')
    assert jobConfig.containsKey('loginPass')
    assert jobConfig.containsKey('open')
    assert jobConfig.containsKey('email')
    assert jobConfig.containsKey('githubRepoUrlSegment')
    assert jobConfig.containsKey('jobName')
    assert jobConfig.containsKey('platformCredential')

    assert ghprbMap.containsKey('admin')
    assert ghprbMap.containsKey('userWhiteList')
    assert ghprbMap.containsKey('orgWhiteList')

    job(jobConfig['jobName']) {

        /* For non-open jobs, enable project based security */
        if (!jobConfig['open'].toBoolean()) {
            authorization {
                blocksInheritance(true)
                permissionAll('edx')
            }
        }

        parameters {
            stringParams.each { param ->
                stringParam(param.name, param.default, param.description)
            }
        }

        properties {
              githubProjectUrl(JENKINS_PUBLIC_GITHUB_BASEURL + jobConfig['githubRepoUrlSegment'])
        }

        logRotator JENKINS_PUBLIC_LOG_ROTATOR() //Discard build after 14 days

        label('jenkins-worker')

        checkoutRetryCount(5)

        scm {

           git { //using git on the branch and url, clone, clean before checkout
                remote {
                    url(JENKINS_PUBLIC_GITHUB_BASEURL + jobConfig['githubRepoUrlSegment'] + '.git')
                    refspec('+refs/pull/*:refs/remotes/origin/pr/*')
                    if (!jobConfig['open'].toBoolean()) {
                        credentials(jobConfig['platformCredential'])
                    }
                }
                branch('${ghprbActualCommit}')
                browser()
            }
        }

        steps {
            shell("jenkins/end_to_end_tests.sh")
        }

        triggers { //triggers on each git pull request
          pullRequest {
              admins(ghprbMap['admin'])
              useGitHubHooks()
              triggerPhrase('jenkins run e2e')
              userWhitelist(ghprbMap['userWhiteList'])
              orgWhitelist(ghprbMap['orgWhiteList'])
              extensions {
                  commitStatus {
                      context('jenkins/e2e')
                  }
              }
          }
        }

        wrappers {
            timeout {
               absolute(75)
            }
            timestamps()
            colorizeOutput('gnome-terminal')
            credentialsBinding {
                string('BASIC_AUTH_USER', jobConfig['authUser'])
                string('BASIC_AUTH_PASSWORD', jobConfig['authPass'])
                string('USER_LOGIN_EMAIL', jobConfig['loginEmail'])
                string('USER_LOGIN_PASSWORD', jobConfig['loginPass'])
            }
        }

        publishers {
            // Achive XML report as XUnit for ease of analysis
            archiveJunit('reports/*.xml') {
                allowEmptyResults(false)
            }
            // Also, archive XML reports so they can be downloaded
            archiveArtifacts('reports/*.xml')
            mailer(jobConfig['email'])
      }
    }
}

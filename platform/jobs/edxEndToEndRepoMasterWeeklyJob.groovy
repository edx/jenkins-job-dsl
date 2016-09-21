package devops

import org.yaml.snakeyaml.Yaml
import static org.edx.jenkins.dsl.JenkinsPublicConstants.JENKINS_PUBLIC_LOG_ROTATOR
import static org.edx.jenkins.dsl.JenkinsPublicConstants.JENKINS_PUBLIC_GITHUB_BASEURL


/*
Example secret YAML file used by this script

    authUser : name-of-auth-user-for-stage
    authPass : password-of-auth-user-for-stage
    loginEmail : email-for-stage-login
    loginPass : password-for-stage-login
    open : true/false
    email: email_for_notification@email.com
    githubRepoUrlSegment : url segment of repo i.e edx/repo_name
    jobName : job-name-to-be-created
    platformCredential: n/a
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

/* Map to hold the k:v pairs parsed from the secret file */
Map secretMap = [:]
out.println('Parsing secret YAML file')
try {
    /* Parse k:v pairs from the secret file referenced by secretFileVariable */
    String contents = new File("${EDX_END_TO_END_TESTS}").text
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
    assert jobConfig.containsKey('authUser')
    assert jobConfig.containsKey('authPass')
    assert jobConfig.containsKey('loginEmail')
    assert jobConfig.containsKey('loginPass')
    assert jobConfig.containsKey('open')
    assert jobConfig.containsKey('email')
    assert jobConfig.containsKey('githubRepoUrlSegment')
    assert jobConfig.containsKey('jobName')
    assert jobConfig.containsKey('platformCredential')

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

        label('jenkins-worker') //restrict to jenkins-worker

        checkoutRetryCount(5)

        environmentVariables {
            env('BASIC_AUTH_USER', jobConfig['authUser'])
            env('BASIC_AUTH_PASSWORD', jobConfig['authPass'])
            env('USER_LOGIN_EMAIL', jobConfig['loginEmail'])
            env('USER_LOGIN_PASSWORD', jobConfig['loginPass'])
        }

        scm {

           git { //using git on the branch and url, clone, clean before checkout
                remote {
                    url(JENKINS_PUBLIC_GITHUB_BASEURL + jobConfig['githubRepoUrlSegment'] + '.git')
                    if (!jobConfig['open'].toBoolean()) {
                        credentials(jobConfig['platformCredential'])
                    }
                }
                branch('*/master')
                browser()
            }
        }

        steps {
            shell("jenkins/end_to_end_tests.sh")
        }

        triggers { //triggers weekly
            scm('@weekly')
        }

        wrappers {
            timeout {
               absolute(75)
            }
            timestamps()
            colorizeOutput('gnome-terminal')
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

package devops

import org.yaml.snakeyaml.Yaml
import static org.edx.jenkins.dsl.JenkinsPublicConstants.JENKINS_PUBLIC_LOG_ROTATOR
import static org.edx.jenkins.dsl.JenkinsPublicConstants.JENKINS_PUBLIC_GITHUB_BASEURL


/*
Example secret YAML file used by this script

    oauth_host : The host used to obtain Course Discovery access token
    oauth_key : Key used to obtain Course Discovery access token
    oauth_secret : Secret used to obtain Course Discovery access token
    sailthru_key : Access key for Sailthru api
    sailthru_secret : Access secret for Sailthru api
    content_api_url : Url of Course Discovery API
    lms_url : Url of LMS (default http://courses.edx.org
    report_email : Email address to sent batch report to
    type : A choice of "programs", "courses" or "all" 
    fixups : any fix
    mode : command mode
    requirements : path to sailthru requirements file in repo
    scriptPath : the script to run for this job
    githubRepoUrlSegment : url segment of repo i.e edx/repo_name
    jobName : job-name-to-be-created
    notificationEmail : Post Action to send email for job notification 
*/

/* stdout logger */
/* use this instead of println, because you can pass it into closures or other scripts. */
Map config = [:]
Binding bindings = getBinding()
config.putAll(bindings.getVariables())
PrintStream out = config['out']

/* Map to hold the k:v pairs parsed from the secret file */
Map secretMap = [:]
out.println('Parsing secret YAML file')
try {
    /* Parse k:v pairs from the secret file referenced by secretFileVariable */
    String contents = new File("${EDX_SAILTHRU_SYNC_CONTENT}").text
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
    assert jobConfig.containsKey('oauth_host')
    assert jobConfig.containsKey('oauth_key')
    assert jobConfig.containsKey('oauth_secret')
    assert jobConfig.containsKey('sailthru_key')
    assert jobConfig.containsKey('sailthru_secret')
    assert jobConfig.containsKey('content_api_url')
    assert jobConfig.containsKey('lms_url')
    assert jobConfig.containsKey('report_email')
    assert jobConfig.containsKey('type')
    assert jobConfig.containsKey('fixups')
    assert jobConfig.containsKey('mode')
    assert jobConfig.containsKey('requirements')
    assert jobConfig.containsKey('scriptPath')
    assert jobConfig.containsKey('githubRepoUrlSegment')
    assert jobConfig.containsKey('jobName')
    assert jobConfig.containsKey('notificationEmail')

    job(jobConfig['jobName']) {

        requirements = "pip install -r " + jobConfig['requirements']
        sync_content = "python " + jobConfig['scriptPath'] + " --oauth_host "+ jobConfig['oauth_host'] + 
                  " --oauth_key " + jobConfig['oauth_key'] + " --oauth_secret " + jobConfig['oauth_secret'] + 
                  " --sailthru_key " + jobConfig['sailthru_key'] + " --sailthru_secret " + jobConfig['sailthru_secret'] +
                  " --content_api_url " + jobConfig['content_api_url'] + " --lms_url " + jobConfig['lms_url'] +
                  " --report_email " + jobConfig['report_email'] + " --fixups=" + jobConfig['fixups'] +
                  " --type " + jobConfig['type'] + " " + jobConfig['mode']

        properties {
              githubProjectUrl(JENKINS_PUBLIC_GITHUB_BASEURL + jobConfig['githubRepoUrlSegment'])
        }

        logRotator JENKINS_PUBLIC_LOG_ROTATOR() //Discard build after 14 days

        label('master') //restrict to jenkins-worker

        checkoutRetryCount(5)

        multiscm {

           git { //using git on the branch and url, clone, clean before checkout
                remote {
                    url(JENKINS_PUBLIC_GITHUB_BASEURL + jobConfig['githubRepoUrlSegment'] + '.git')
                }
                branch('*/master')
            }
        }

        steps {
            virtualenv {
                pythonName('System-CPython-2.7')
                nature('shell')
                command( requirements )
                command( sync_content )
            }
        }

        triggers { //triggers daily
            scm('@daily')
        }

        wrappers {
            timeout {
               absolute(75)
            }
            timestamps()
            colorizeOutput('gnome-terminal')
        }

        publishers {
            mailer(jobConfig['notificationEmail'])
      }
    }
}

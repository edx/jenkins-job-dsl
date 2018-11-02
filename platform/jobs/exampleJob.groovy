package devops

/* JenkinsPublicConstants can be found in src/main/groovy/org/edx/jenkins/dsl*/
/* It contains values and methods that are used repeatedly through the dsl jobs */
import org.yaml.snakeyaml.Yaml
import static org.edx.jenkins.dsl.JenkinsPublicConstants.JENKINS_PUBLIC_LOG_ROTATOR
import static org.edx.jenkins.dsl.JenkinsPublicConstants.JENKINS_PUBLIC_WORKER
import static org.edx.jenkins.dsl.JenkinsPublicConstants.GENERAL_SLACK_STATUS
import static org.edx.jenkins.dsl.JenkinsPublicConstants.JENKINS_PUBLIC_GITHUB_BASEURL

/*
Example secret YAML file used by this script
publicJobConfig:
    open : true/false
    jobName : name-of-jenkins-job-to-be
    credential : n/a
*/

/* stdout logger */
/* Use this instead of println, because you can pass it into closures or other scripts. */
Map config = [:]
Binding bindings = getBinding()
config.putAll(bindings.getVariables())
PrintStream out = config['out']

/* Parameters for the job */
params = [
    name: 'sha1',
    description: 'Sha1 hash of branch to build. Default branch : master',
    default: 'refs/heads/master' ]

/* Map to hold the k:v pairs parsed from the secret file */
Map secretMap = [:]
try {
    out.println('Parsing secret YAML file')
    /* Parse k:v pairs from the secret file referenced by secret file variable in the seed job */
    String contents = new File("${SECRET_FILE_NAME}").text
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

    /* Test that secret contains all necessary keys for this job */
    assert jobConfig.containsKey('open')
    assert jobConfig.containsKey('jobName')
    assert jobConfig.containsKey('credential')

    /* Set the job name */
    job(jobConfig['jobName']) {

        /* For non-open jobs, enable project based security */
        if (!jobConfig['open'].toBoolean()) {
            authorization {
                blocksInheritance(true)
                permissionAll('edx')
            }
        }
        /* Job Set Up */
        /* Set the repository being used, add parameters to the job, keep the logging information
           for a set duration, run the job concurrently, and only run on a specific worker node
           i.e. jenkins-worker */
        properties {
            githubProjectUrl(JENKINS_PUBLIC_GITHUB_BASEURL + 'org/repositoryName')
        }
        parameters {
            stringParam(params.name, params.default, params.description)
        }
        logRotator JENKINS_PUBLIC_LOG_ROTATOR()
        concurrentBuild()
        label(JENKINS_PUBLIC_WORKER)

        /* Using Source Code Management */
        /* Set the repository, if it is  private respository, set credentials for access, specify branch */
        scm {
            git {
                remote {
                    url(JENKINS_PUBLIC_GITHUB_BASEURL + 'org/repositoryName.git')
                    /* If the repository is a private, set credentials to use */
                    if (!jobConfig['open'].toBoolean()) {
                        credentials(jobConfig['credential'])
                    }
                }
                branch('\${sha1}')
            }
        }

        /* Trigger a job automatically when a change is pushed to Github */
        triggers {
            githubPush()
        }

        /* Format console output */
        /* Abort a stuck build after x minutes, use gnome-terminal coloring, have timestamps */
        wrappers {
            timeout {
               absolute(75)
           }
           timestamps()
           colorizeOutput('gnome-terminal')
       }

       /* Build Steps */
       steps {
           shell("String of shell commands, separate lines with a ';' ")
       }

       /* Post Build Steps */
       /* Archive artifacts, archive jUnit Reports, send an email, message on slack */
       publishers {
           archiveArtifacts {
               pattern('Comma separated list of files to archive')
               allowEmpty()
               defaultExcludes()
           }
           archiveJunit('Comma separated list of files to archive')
           mailer('email@email.com')
           configure GENERAL_SLACK_STATUS()
       }
    }
}

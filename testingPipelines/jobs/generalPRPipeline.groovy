import org.yaml.snakeyaml.Yaml
import static org.edx.jenkins.dsl.JenkinsPublicConstants.GENERAL_PRIVATE_JOB_SECURITY
import static org.edx.jenkins.dsl.JenkinsPublicConstants.JENKINS_PUBLIC_LOG_ROTATOR
import static org.edx.jenkins.dsl.JenkinsPublicConstants.GHPRB_CANCEL_BUILDS_ON_UPDATE

/* stdout logger */
Map config = [:]
Binding bindings = getBinding()
config.putAll(bindings.getVariables())
PrintStream out = config['out']

/* Map to hold the k:v pairs parsed from the secret file */
Map ghprbMap = [:]
try {
    out.println('Parsing secret YAML file')
    String ghprbConfigContents = new File("${GHPRB_SECRET}").text
    Yaml yaml = new Yaml()
    ghprbMap = yaml.load(ghprbConfigContents)
    out.println('Successfully parsed secret YAML file')
}
catch (any) {
    out.println('Jenkins DSL: Error parsing secret YAML file')
    out.println('Exiting with error code 1')
    return 1
}

Map codejail38TestsJobConfig = [
    open: true,
    jobName: 'codejail-python-3.8-pr-tests',
    repoName: 'codejail',
    context: 'jenkins/python3.8/tests',
    onlyTriggerPhrase: false,
    triggerPhrase: /.*jenkins\W+run\W+py38\W+tests.*/,
    jenkinsFileDir: '.',
    jenkinsFileName: 'Jenkinsfile',
    environmentVariables: [
        'TOX_ENV': 'py38',
        'PYTHON_VERSION': '3.8'
    ]
]

Map codejail38QualityJobConfig = [
    open: true,
    jobName: 'codejail-python-3.8-pr-quality',
    repoName: 'codejail',
    context: 'jenkins/python3.8/quality',
    onlyTriggerPhrase: false,
    triggerPhrase: /.*jenkins\W+run\W+py38\W+quality.*/,
    jenkinsFileDir: '.',
    jenkinsFileName: 'Jenkinsfile',
    environmentVariables: [
        'TOX_ENV': 'quality',
        'PYTHON_VERSION': '3.8'
    ]
]


List jobConfigs = [
    codejail38TestsJobConfig,
    codejail38QualityJobConfig
]

/* Iterate over the job configurations */
jobConfigs.each { jobConfig ->

    // This is the job DSL responsible for creating the main pipeline job.
    pipelineJob(jobConfig.jobName) {

        definition {

            if (!jobConfig.open.toBoolean()) {
                authorization GENERAL_PRIVATE_JOB_SECURITY()
            }
            properties {
                githubProjectUrl("https://github.com/edx/${jobConfig.repoName}/")
            }
            logRotator JENKINS_PUBLIC_LOG_ROTATOR(7)
            environmentVariables jobConfig.environmentVariables


            triggers {
                githubPullRequest {
                    admins(ghprbMap['admin'])
                    useGitHubHooks()
                    triggerPhrase(jobConfig.triggerPhrase)
                    onlyTriggerPhrase(jobConfig.onlyTriggerPhrase)
                    userWhitelist(ghprbMap['userWhiteList'])
                    orgWhitelist(ghprbMap['orgWhiteList'])
                    extensions {
                        commitStatus {
                            context(jobConfig.context)
                        }
                    }
                }
            }

            configure GHPRB_CANCEL_BUILDS_ON_UPDATE(false)

            cpsScm {
                scm {
                    git {
                        extensions {
                            cleanBeforeCheckout()
                            cloneOptions {
                                honorRefspec(true)
                                noTags(true)
                                shallow(true)
                            }
                        }
                        remote {
                            credentials('jenkins-worker')
                            github("edx/${jobConfig.repoName}", 'ssh', 'github.com')
                            refspec('+refs/pull/${ghprbPullId}/*:refs/remotes/origin/pr/${ghprbPullId}/*')
                            branch('\${sha1}')
                        }
                    }
                }
                scriptPath(jobConfig.jenkinsFileDir + '/' + jobConfig.jenkinsFileName)
            }
        }
    }
}


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
// try {
//     out.println('Parsing secret YAML file')
//     String ghprbConfigContents = new File("${GHPRB_SECRET}").text
//     Yaml yaml = new Yaml()
//     ghprbMap = yaml.load(ghprbConfigContents)
//     out.println('Successfully parsed secret YAML file')
// }
// catch (any) {
//     out.println('Jenkins DSL: Error parsing secret YAML file')
//     out.println('Exiting with error code 1')
//     return 1
// }

Map cypressPipelineJob = [
    open: true,
    jobName: 'cypress-tests',
    repoName: 'cypress-e2e-tests',
    context: 'jenkins/cypress',
    worker: 'jenkins-worker',
    trigger: 'pipeline',
    onlyTriggerPhrase: false,
    triggerPhrase: /.*jenkins\W+run\W+cypress.*/,
    jenkinsFileDir: '.',
    jenkinsFileName: 'Jenkinsfile',
]

Map cypressPrJob = [
    open: true,
    jobName: 'cypress-tests-pr',
    repoName: 'cypress-e2e-tests',
    worker: 'jenkins-worker',
    context: 'jenkins/cypress',
    trigger: 'ghprb',
    triggerPhrase: /.*jenkins\W+run\W+cypress.*/,
    branch: '${ghprbActualCommit}',
    refspec: '+refs/pull/*:refs/remotes/origin/pr/*',
    description: 'Verify the quality of changes made to the microsite tests',
    onlyTriggerPhrase: false,
    jenkinsFileDir: '.',
    jenkinsFileName: 'Jenkinsfile',
]

Map cypressMergeJob = [
    open: true,
    jobName: 'cypress-tests-merge',
    repoName: 'cypress-e2e-tests',
    context: 'jenkins/cypress',
    worker: 'jenkins-worker',
    trigger: 'Merge',
    triggerPhrase: /.*jenkins\W+run\W+cypress.*/,
    branch: '${ghprbActualCommit}',
    refspec: '+refs/heads/master:refs/remotes/origin/master',
    description: 'Verify the quality of changes made to the cypress tests',
    onlyTriggerPhrase: false,
    jenkinsFileDir: '.',
    jenkinsFileName: 'Jenkinsfile',
]

List jobConfigs = [
    cypressPipelineJob,
    cypressPrJob,
    cypressMergeJob
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
            

            if (jobConfig.trigger == 'ghprb') {
                triggers {
                    githubPullRequest {
                        admins(ghprbMap['admin'])
                        useGitHubHooks()
                        triggerPhrase(jobConfig.triggerPhrase)
                        userWhitelist(ghprbMap['userWhiteList'])
                        orgWhitelist(ghprbMap['orgWhiteList'])
                        whiteListTargetBranches([jobConfig.branchRegex])
                        extensions {
                            commitStatus {
                                context(jobConfig.context)
                            }
                        }
                    }
                }
                configure GHPRB_CANCEL_BUILDS_ON_UPDATE(false)
            }
            // enable triggers for merge jobs
            else if (jobConfig.trigger == 'merge') {
                triggers {
                    // due to a bug or misconfiguration, jobs with branches with
                    // slashes are indiscriminately triggered by pushes to other branches.
                    // For more information, see:
                    // https://openedx.atlassian.net/browse/TE-1921
                    // for commits merging into master, trigger jobs via github pushes
                    if (!jobConfig.deprecated) {
                        githubPush()
                    }
                    // for merges to other non-master branches, poll instead
                    else {
                        scm('@hourly')
                    }
                }
            }

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
                            if (jobConfig.trigger == 'merge') {
                                    branch(jobConfig.branch)
                            }
                            else {
                                branch('\${sha1}')
                            }
                        }
                    }
                }
                scriptPath(jobConfig.jenkinsFileDir + '/' + jobConfig.jenkinsFileName)
            }
        }
    }
}





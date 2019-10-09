import org.yaml.snakeyaml.Yaml
import static org.edx.jenkins.dsl.JenkinsPublicConstants.GENERAL_PRIVATE_JOB_SECURITY
import static org.edx.jenkins.dsl.JenkinsPublicConstants.JENKINS_PUBLIC_LOG_ROTATOR
import static org.edx.jenkins.dsl.JenkinsPublicConstants.GHPRB_CANCEL_BUILDS_ON_UPDATE

/* stdout logger */
Map config = [:]
Binding bindings = getBinding()
config.putAll(bindings.getVariables())
PrintStream out = config['out']

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
    job(jobConfig.jobName) {

        description(jobConfig.description)

        authorization {
            blocksInheritance(true)
            permissionAll('edx')
            permission('hudson.model.Item.Discover', 'anonymous')
            // grant additional permissions to bots
            if (jobConfig.trigger == 'pipeline') {
                List<String> permissionList = [ 'hudson.model.Item.Read',
                                                'hudson.model.Item.Workspace',
                                                'hudson.model.Item.Discover',
                                                'hudson.model.Item.Build',
                                                'hudson.model.Item.Cancel' ]
                permissionList.each { perm ->
                    permission(perm, securityGroupMap['e2eSecurityGroup'])
                }
            }
        }

        if (jobConfig.trigger == 'ghprb' || jobConfig.trigger == 'merge') {
            properties {
                githubProjectUrl('https://github.com/edx/${jobConfig.repoName}')
            }
        }
            

            // enable triggers for the PR jobs
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

        scm {
            git {
                remote {
                    url('https://github.com/edx/${jobConfig.repoName}.git')
                    refspec(jobConfig.refspec)
                }
                if (jobConfig.trigger == 'merge') {
                    branch(jobConfig.branch)
                }
                else {
                    branch('\${sha1}')
                }
            }
        }
        scriptPath(jobConfig.jenkinsFileDir + '/' + jobConfig.jenkinsFileName)
        }
    }
}

jobConfigs.each { jobConfig ->
    job(jobConfig.name) {

        description(jobConfig.description)

        authorization {
            blocksInheritance(true)
            permissionAll('edx')
            permission('hudson.model.Item.Discover', 'anonymous')
            // grant additional permissions to bots
            if (jobConfig.trigger == 'pipeline') {
                List<String> permissionList = [ 'hudson.model.Item.Read',
                                                'hudson.model.Item.Workspace',
                                                'hudson.model.Item.Discover',
                                                'hudson.model.Item.Build',
                                                'hudson.model.Item.Cancel' ]
                permissionList.each { perm ->
                    permission(perm, securityGroupMap['e2eSecurityGroup'])
                }
            }
        }

        if (jobConfig.trigger == 'ghprb' || jobConfig.trigger == 'merge') {
            properties {
                githubProjectUrl("https://github.com/edx/${jobConfig.repoName}/")
            }
        }

        logRotator JENKINS_PUBLIC_LOG_ROTATOR()
        label(jobConfig.worker)
        // Disable concurrent builds because the environment in use is a shared
        // resource, and concurrent builds can cause spurious test results
        concurrentBuild(false)

        scm {
            git {
                remote {
                    url("https://github.com/edx/${jobConfig.repoName}/")
                    refspec(jobConfig.refspec)
                }
                credentials('jenkins-worker')
                github("edx/${jobConfig.repoName}", 'ssh', 'github.com')
                if (jobConfig.trigger == 'merge') {
                    branch(jobConfig.branch)
                }
                else {
                    branch('\${sha1}')
                }
            }
        }
        scriptPath(jobConfig.jenkinsFileDir + '/' + jobConfig.jenkinsFileName)

        // enable triggers for the PR jobs
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
        wrappers {
            timeout {
                absolute(75)
            }
            timestamps()
            colorizeOutput('gnome-terminal')
        }

        steps {
            // Use a different set of user accounts for non-deployment jobs to avoid
            // conflicts
            if (jobConfig.trigger == 'merge' || jobConfig.trigger == 'ghprb') {
                environmentVariables {
                    env('USER_DATA_SET', 'pr')
                 }
            }
            shell(jobConfig.testScript)
        }


        publishers {
            // for merge jobs, update github with the test status
            if (jobConfig.trigger == 'pipeline') {
                mailer(mailingListMap['e2e_test_mailing_list'])
            }
        }

    }
}



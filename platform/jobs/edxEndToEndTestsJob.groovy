package platform

import org.yaml.snakeyaml.Yaml
import static org.edx.jenkins.dsl.JenkinsPublicConstants.JENKINS_PUBLIC_LOG_ROTATOR
import static org.edx.jenkins.dsl.JenkinsPublicConstants.JENKINS_PUBLIC_WORKER

/* stdout logger */
/* use this instead of println, because you can pass it into closures or other scripts. */
Map config = [:]
Binding bindings = getBinding()
config.putAll(bindings.getVariables())
PrintStream out = config['out']

/* Map to hold the k:v pairs parsed from the secret file */
Map mailingListMap = [:]
Map ghprbMap = [:]
Map securityGroupMap = [:]
try {
    out.println('Parsing secret YAML file')
    String mailingListSecretContents  = new File("${MAILING_LIST_SECRET}").text
    String ghprbConfigContents = new File("${GHPRB_SECRET}").text
    String securityGroupConfigContents = new File("${SECURITY_GROUP_SECRET}").text
    Yaml yaml = new Yaml()
    mailingListMap = yaml.load(mailingListSecretContents)
    ghprbMap = yaml.load(ghprbConfigContents)
    securityGroupMap = yaml.load(securityGroupConfigContents)
    out.println('Successfully parsed secret YAML file')
}
catch (any) {
    out.println('Jenkins DSL: Error parsing secret YAML file')
    out.println('Exiting with error code 1')
    return 1
}

assert mailingListMap.containsKey('e2e_test_mailing_list')
assert securityGroupMap.containsKey('e2eSecurityGroup')
assert ghprbMap.containsKey('admin')
assert ghprbMap.containsKey('userWhiteList')
assert ghprbMap.containsKey('orgWhiteList')

// The edx-e2e-tests job is run automatically on every deployment of the edx-platform
// from the gocd pipeline.
Map pipelineJob = [ name: 'edx-e2e-tests',
                    trigger: 'pipeline',
                    branch: '*/master',
                    description: 'Run end-to-end tests against GoCD deployments',
                    courseNumber: 'AR-1000'
                    ]

// The edx-e2e-tests_pr job is run on every PR to the edx-e2e-tests repo. This
// is used for development of the tests themselves
Map prJob = [ name: 'edx-e2e-tests-pr',
              trigger: 'ghprb',
              branch: '${ghprbActualCommit}',
              description: 'Verify the quality of changes made to the end-to-end tests',
              courseNumber: 'AR-1001'
              ]

List jobConfigs = [ pipelineJob, prJob ]

jobConfigs.each { jobConfig ->
    job(jobConfig.name) {

        description(jobConfig.description)

        authorization {
            blocksInheritance(true)
            permissionAll('edx')
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

        if (jobConfig.trigger == 'ghprb') {
            properties {
                githubProjectUrl('https://github.com/edx/edx-e2e-tests')
            }
        }

        logRotator JENKINS_PUBLIC_LOG_ROTATOR()
        label(JENKINS_PUBLIC_WORKER)
        // Disable concurrent builds because the environment in use is a shared
        // resource, and concurrent builds can cause spurious test results
        concurrentBuild(false)

        parameters {
            stringParam('COURSE_ORG', 'ArbiRaees', 'Organization name of the course')
            stringParam('COURSE_NUMBER', jobConfig.courseNumber, 'Course number')
            stringParam('COURSE_RUN', 'fall', 'Term in which course will run')
            stringParam('COURSE_DISPLAY_NAME', 'Manual Smoke Test Course 1 - Auto', 'Display name of the course')
            // Both the pipeline and pr jobs are inteded to be triggered via external
            // automation, but this parameter allows the default values to be overriden
            // when run manually.
            stringParam('E2E_BRANCH', jobConfig.branch, 'Branch of the e2e test repo to use')
        }

        scm {
            git {
                remote {
                    url('https://github.com/edx/edx-e2e-tests.git')
                    refspec('+refs/pull/*:refs/remotes/origin/pr/*')
                }
                branch('\${E2E_BRANCH}')
                browser()
            }
        }

        // enable triggers for the PR job
        if (jobConfig.trigger == 'ghprb') {
            triggers {
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
        }

        wrappers {
            timeout {
                absolute(75)
            }
            timestamps()
            colorizeOutput('gnome-terminal')
            credentialsBinding {
                string('BASIC_AUTH_USER', 'BASIC_AUTH_USER')
                string('BASIC_AUTH_PASSWORD', 'BASIC_AUTH_PASSWORD')
                string('USER_LOGIN_EMAIL', 'USER_LOGIN_EMAIL')
                string('USER_LOGIN_PASSWORD', 'USER_LOGIN_PASSWORD')
            }
        }

        steps {
            shell('jenkins/end_to_end_tests.sh')
        }

        publishers {
            archiveJunit('reports/*.xml') {
                allowEmptyResults(false)
            }
            archiveArtifacts {
                pattern('reports/*.xml')
                pattern('log/*')
                pattern('screenshots/*')
            }
            mailer(mailingListMap['e2e_test_mailing_list'])
        }

    }
}

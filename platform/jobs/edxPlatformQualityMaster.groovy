package devops

import org.yaml.snakeyaml.Yaml
import static org.edx.jenkins.dsl.JenkinsPublicConstants.JENKINS_PUBLIC_LOG_ROTATOR
import static org.edx.jenkins.dsl.JenkinsPublicConstants.JENKINS_PUBLIC_HIPCHAT
import static org.edx.jenkins.dsl.JenkinsPublicConstants.JENKINS_PUBLIC_BASE_URL
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
    email : email-address@email.com
    hipchat : token
    workerLabel : worker-label
    refSpec : '+refs/heads/master:refs/remotes/origin/master'
    context : 'jenkins/test'
    defaultBranch : 'master'
    disabled: true/false
*/

String archiveReports = 'edx-platform*/reports/**/*,edx-platform*/test_root/log/*.png,'
archiveReports += 'edx-platform*/test_root/log/*.log, edx-platform*/test_root/log/hars/*.har,'
archiveReports += 'edx-platform*/**/nosetests.xml,edx-platform*/**/TEST-*.xml'

String htmlReports = 'pylint/*view*/, pep8/*view*/, jshint/*view*/, python_complexity/*view*/,'
htmlReports += 'xsscommitlint/*view*/, xsslint/*view*/, eslint/*view*/'

/* stdout logger */
/* use this instead of println, because you can pass it into closures or other scripts. */
/* TODO: Move this into JenkinsPublicConstants, as it can be shared. */
Map config = [:]
Binding bindings = getBinding()
config.putAll(bindings.getVariables())
PrintStream out = config['out']

/* Map to hold the k:v pairs parsed from the secret file */
Map secretMap = [:]
try {
    out.println('Parsing secret YAML file')
    /* Parse k:v pairs from the secret file referenced by secretFileVariable */
    String contents = new File("${EDX_PLATFORM_TEST_QUALITY_SECRET}").text
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
                permission('hudson.model.Item.Discover', 'anonymous')
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
        logRotator JENKINS_PUBLIC_LOG_ROTATOR() //Discard build after 14 days
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
                        reference("\$HOME/" + jobConfig['cloneReference'])
                        timeout(10)
                    }
                    cleanBeforeCheckout()
                    relativeTargetDirectory(jobConfig['repoName'])
                }
            }
        }
        triggers {
            // Trigger jobs via github pushes
            githubPush()
        }
        wrappers { //abort when stuck after 90 minutes, x-mal coloring, timestamps at Console, change the build name
            timeout {
               absolute(90)
           }
           timestamps()
           colorizeOutput()
           if (!jobConfig['open'].toBoolean()) {
                sshAgent(jobConfig['credential'])
           }
           buildName('#${BUILD_NUMBER}: Quality Tests')
       }

        Map <String, String> predefinedPropsMap  = [:]
        predefinedPropsMap.put('GIT_SHA', '${GIT_COMMIT}')
        predefinedPropsMap.put('GITHUB_ORG', 'edx')
        predefinedPropsMap.put('CONTEXT', jobConfig['context'])
        steps { //trigger GitHub-Build-Status and run accessibility tests
            predefinedPropsMap.put('GITHUB_REPO', jobConfig['repoName'])
            predefinedPropsMap.put('TARGET_URL', JENKINS_PUBLIC_BASE_URL + 'job/'
                                  + jobConfig['jobName'] + '/${BUILD_NUMBER}/')
            downstreamParameterized JENKINS_PUBLIC_GITHUB_STATUS_PENDING.call(predefinedPropsMap)
            shell("cd ${jobConfig['repoName']}; TEST_SUITE=quality ./scripts/all-tests.sh")
        }
        publishers { //publish artifacts, HTML, violations report, trigger GitHub-Build-Status, email, message hipchat
            archiveArtifacts {
                pattern(archiveReports)
                defaultExcludes()
            }
            publishHtml {
                report(jobConfig['repoName'] + '/reports/metrics/') {
                    reportFiles(htmlReports)
                    reportName('Quality Report')
                    allowMissing()
                    keepAll()
                }
            }
            violations(100) {
                checkstyle(10, 999, 999)
                codenarc(10, 999, 999)
                cpd(10, 999, 999)
                cpplint(10, 999, 999)
                csslint(10, 999, 999)
                findbugs(10, 999, 999)
                fxcop(10, 999, 999)
                gendarme(10, 999, 999)
                jcreport(10, 999, 999)
                jslint(10, 999, 999)
                pep8(1, 2, 3, '**/pep8.report')
                perlcritic(10, 999, 999)
                pmd(10, 999, 999)
                pylint(10, 10000, 10000, '**/*pylint.report')
                simian(10, 999, 999)
                stylecop(10, 999, 999)
                sourceEncoding()
            }
            downstreamParameterized JENKINS_PUBLIC_GITHUB_STATUS_SUCCESS.call(predefinedPropsMap)
            downstreamParameterized JENKINS_PUBLIC_GITHUB_STATUS_UNSTABLE_OR_WORSE.call(predefinedPropsMap)
            mailer(jobConfig['email'])
            hipChat JENKINS_PUBLIC_HIPCHAT.call(jobConfig['hipchat'])
        }
    }
}

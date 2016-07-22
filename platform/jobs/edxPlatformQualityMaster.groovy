package devops

import org.yaml.snakeyaml.Yaml
import static org.edx.jenkins.dsl.JenkinsPublicConstants.JENKINS_PUBLIC_LOG_ROTATOR
import static org.edx.jenkins.dsl.JenkinsPublicConstants.JENKINS_PUBLIC_WORKER
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
*/

Map <String, String> predefinedPropsMap  = [:]
predefinedPropsMap.put('GIT_SHA', '${GIT_COMMIT}')
predefinedPropsMap.put('GITHUB_ORG', 'edx')
predefinedPropsMap.put('GITHUB_REPO', 'edx-platform')
predefinedPropsMap.put('TARGET_URL', JENKINS_PUBLIC_BASE_URL + 'job/edx-platform-quality-master/${BUILD_NUMBER}/')
predefinedPropsMap.put('CONTEXT', 'jenkins/quality')

String archiveReports = 'edx-platform*/reports/**/*,edx-platform*/test_root/log/*.png,'
archiveReports += 'edx-platform*/test_root/log/*.log, edx-platform*/test_root/log/hars/*.har,'
archiveReports += 'edx-platform*/**/nosetests.xml,edx-platform*/**/TEST-*.xml'

String htmlReports = 'pylint/*view*/, pep8/*view*/, jshint/*view*/, python_complexity/*view*/,'
htmlReports += 'safecommit/*view*/, safelint/*view*/'

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

    job(jobConfig['jobName']) {

        /* For non-open jobs, enable project based security */
        if (!jobConfig['open'].toBoolean()) {
            authorization {
                blocksInheritance(true)
                permissionAll('edx')
            }
        }
        properties {
            githubProjectUrl(JENKINS_PUBLIC_GITHUB_BASEURL + jobConfig['url'])
        }
        parameters {
            stringParam(params.name, params.default, params.description)
        }
        logRotator JENKINS_PUBLIC_LOG_ROTATOR() //Discard build after 14 days
        concurrentBuild() //concurrent builds can happen
        label(JENKINS_PUBLIC_WORKER) //restrict to jenkins-worker
        scm {
            git { //using git on the branch and url, clone, clean before checkout
                remote {
                    url(jobConfig['protocol'] + jobConfig['url'] + '.git')
                    refspec('+refs/heads/master:refs/remotes/origin/master')
                    if (!jobConfig['open'].toBoolean()) {
                        credentials(jobConfig['credential'])
                    }
                }
                branch('\${sha1}')
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
        triggers { //trigger when change pushed to GitHub
            githubPush()
        }
        wrappers { //abort when stuck after 45 minutes, x-mal coloring, timestamps at Console, change the build name
            timeout {
               absolute(45)
           }
           timestamps()
           colorizeOutput()
           if (!jobConfig['open'].toBoolean()) {
                sshAgent(jobConfig['credential'])
           }
           buildName('#${BUILD_NUMBER}: Quality Tests')
       }
       steps { //trigger GitHub-Build-Status and run accessibility tests
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
               pylint(10, 4500, 4500, '**/*pylint.report')
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

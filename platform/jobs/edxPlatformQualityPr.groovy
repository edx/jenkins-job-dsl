package platform

import org.yaml.snakeyaml.Yaml
import static org.edx.jenkins.dsl.JenkinsPublicConstants.JENKINS_PUBLIC_LOG_ROTATOR
import static org.edx.jenkins.dsl.JenkinsPublicConstants.JENKINS_PUBLIC_GITHUB_BASEURL

/*
Example secret YAML file used by this script
publicJobConfig:
    open : true/false
    jobName : name-of-jenkins-job-to-be
    repoName : name-of-github-edx-repo
    platformUrl : platform-github-url-segment.git
    platformCredential : n/a
    platformCloneReference : clone/.git
    protocol : https
*/

/* stdout logger */
/* use this instead of println, because you can pass it into closures or other scripts. */
/* TODO: Move this into JenkinsPublicConstants, as it can be shared. */
Map config = [:]
Binding bindings = getBinding()
config.putAll(bindings.getVariables())
PrintStream out = config['out']

/* Map to hold the k:v pairs parsed from the secret file */
Map secretMap = [:]
Map ghprbMap = [:]
try {
    out.println('Parsing secret YAML file')
    /* Parse k:v pairs from the secret file referenced by secretFileVariable */
    String secretFileContents = new File("${EDX_PLATFORM_QUALITY_PR_SECRET}").text
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

    /* Test secret contains all necessary keys for this job */
    assert jobConfig.containsKey('open')

    job(jobConfig['jobName']) {

        /* For non-open jobs, enable project based security */
        if (!jobConfig['open'].toBoolean()) {
            authorization {
                blocksInheritance(true)
                permissionAll('edx')
            }
        }
        description('This job runs pull requests through our quality checks.')
        logRotator JENKINS_PUBLIC_LOG_ROTATOR()
        properties {
              githubProjectUrl(JENKINS_PUBLIC_GITHUB_BASEURL + jobConfig['platformUrl'])
        }
        concurrentBuild()
        label('jenkins-worker')
        scm {
            git { //using git on the branch and url, clone, clean before checkout
                remote {
                    url(jobConfig['protocol'] + "://github.com/" + jobConfig['platformUrl'] + '.git')
                    refspec('+refs/pull/*:refs/remotes/origin/pr/*')
                    if (!jobConfig['open'].toBoolean()) {
                        credentials(jobConfig['platformCredential'])
                    }
                }
                branch('\${ghprbActualCommit}')
                browser()
                extensions {
                    cloneOptions {
                        reference("\$HOME/" + jobConfig['platformCloneReference'])
                        timeout(10)
                    }
                    cleanBeforeCheckout()
                    relativeTargetDirectory(jobConfig['repoName'])
                }
            }
        }
        triggers { //trigger when pull request is created
           pullRequest {
               admins(ghprbMap['admin'])
                useGitHubHooks()
                triggerPhrase('jenkins run quality')
                // skip phrase
                userWhitelist(ghprbMap['userWhiteList'])
                orgWhitelist(ghprbMap['orgWhiteList'])
               extensions {
                  commitStatus {
                        context('jenkins/quality')
                    }
                }
             }
        }
        wrappers {
            timeout {
                absolute(45)
            }
            timestamps()
            colorizeOutput()
            buildName('#\${BUILD_NUMBER}: Quality Tests')
        }
        steps {
            shell("cd ${jobConfig['repoName']}; TEST_SUITE=quality ./scripts/all-tests.sh")
        }
        publishers {
            archiveArtifacts {
                pattern('edx-platform*/reports/**/*,edx-platform*/test_root/log/*.png,edx-platform*/test_root/log/*.log,edx-platform*/test_root/log/hars/*.har,edx-platform*/**/nosetests.xml,edx-platform*/**/TEST-*.xml')
                defaultExcludes(true)
                allowEmpty(true)
            }
            publishHtml {
                report('edx-platform*/reports/metrics/') {
                    reportName('Quality Report')
                    reportFiles('pylint/*view*/,pep8/*view*/,jshint/*view*/,python_complexity/*view*/,safecommit/*view*/,safelint/*view*/')
                    keepAll(true)
                    allowMissing(true)
                }
                report('edx-platform*/reports/diff_quality') {
                    reportName('Diff Quality Report')
                    reportFiles('diff_quality_pep8.html,diff_quality_pylint.html,diff_quality_jshint.html')
                    keepAll(true)
                }
            }
        }
    }
}

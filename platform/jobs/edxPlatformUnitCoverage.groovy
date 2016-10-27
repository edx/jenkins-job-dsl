package devops

import org.yaml.snakeyaml.Yaml
import static org.edx.jenkins.dsl.JenkinsPublicConstants.JENKINS_PUBLIC_GITHUB_BASEURL

/*
Example secret YAML file used by this script
publicJobConfig:
    open : true/false
    jobName : name-of-jenkins-job-to-be
    protocol : protocol-and-base-url
    url : github-url-segment
    credential : n/a
    cloneReference : clone/.git
    subsetJob : name-of-test-subset-job
*/

/* stdout logger */
Map config = [:]
Binding bindings = getBinding()
config.putAll(bindings.getVariables())
PrintStream out = config['out']

stringParams = [
    [
        name: 'sha1',
        description: '',
        default: ''
    ],
    [
        name: 'UNIT_BUILD_NUM_1',
        description: '',
        default: ''
    ],
    [
        name: 'UNIT_BUILD_NUM_2',
        description: '',
        default: ''
    ],
    [
        name: 'UNIT_BUILD_NUM_3',
        description: '',
        default: ''
    ],
    [
        name: 'UNIT_BUILD_NUM_4',
        description: '',
        default: ''
    ],
    [
        name: 'UNIT_BUILD_NUM_5',
        description: '',
        default: ''
    ],
    [
        name: 'UNIT_BUILD_NUM_6',
        description: '',
        default: ''
    ],
    [
        name: 'PARENT_BUILD',
        description: 'Solution to <a href=\"https://openedx.atlassian.net/browse/TE-894\">TE-894</a>. ' +
                     'Leave as default if starting a build manually.',
        default: '0'
    ],
    [
        name: 'CI_BRANCH',
        description: '',
        default: 'unspecified'
    ]
]

Map secretMap = [:]
try {
    out.println('Parsing secret YAML file')
    /* Parse k:v pairs from the secret file referenced by secretFileVariable */
    String contents = new File("${EDX_PLATFORM_UNIT_COVERAGE_SECRET}").text
    Yaml yaml = new Yaml()
    secretMap = yaml.load(contents)
    out.println('Successfully parsed secret YAML file')
}
catch (any) {
    out.println(any)
    out.println('Jenkins DSL: Error parsing secret YAML file')
    out.println('Exiting with error code 1')
    return 1
}

/* Iterate over the job configurations */
secretMap.each { jobConfigs ->

    Map jobConfig = jobConfigs.getValue()

    /* Test secret contains all necessary keys for this job */
    assert jobConfig.containsKey('open')
    assert jobConfig.containsKey('jobName')
    assert jobConfig.containsKey('url')
    assert jobConfig.containsKey('credential')
    assert jobConfig.containsKey('cloneReference')
    assert jobConfig.containsKey('subsetJob')

    job(jobConfig['jobName']) {

        logRotator {
            daysToKeep(14)
            numToKeep(-1)
            artifactDaysToKeep(5)
            artifactNumToKeep(-1)
        }
        properties {
            githubProjectUrl(JENKINS_PUBLIC_GITHUB_BASEURL + jobConfig['url'])
        }
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
        concurrentBuild(true)
        label('coverage-worker')
        environmentVariables {
            env('SUBSET_JOB', jobConfig['subsetJob'])
        }
        scm {
            git {
                remote {
                    url(jobConfig['protocol'] + jobConfig['url'] + '.git')
                    refspec('+refs/heads/*:refs/remotes/origin/* +refs/pull/*:refs/remotes/origin/pr/*')
                    if (!jobConfig['open'].toBoolean()) {
                        credentials(jobConfig['credential'])
                    }
                }
                browser()
                branch('\${sha1}')
                extensions {
                    cloneOptions {
                        shallow(false)
                        reference('\$HOME/'.concat(jobConfig['cloneReference']))
                        timeout(10)
                    }
                    cleanBeforeCheckout()
                }
            }
        }
        wrappers {
            timeout {
                absolute(20)
                writeDescription('Timed out at 20 minutes')
                abortBuild()
            }
            timestamps()
            colorizeOutput('gnome-terminal')
            if (!jobConfig['open'].toBoolean()) {
                sshAgent(jobConfig['credential'])
            }
            buildName('#\${BUILD_NUMBER}: \${GIT_REVISION,length=8}')
            // Inject CodeCov token so that public jobs can report coverage
            if (jobConfig['open'].toBoolean()) {
                credentialsBinding {
                    string('CODE_COV_TOKEN', 'CODE_COV_TOKEN')
                }
            }
        }
       /* Copy Artifacts from test subset jobs with build number UNIT_BUILD_NUM */
        steps {
            for (buildNum = 1; buildNum < 7; buildNum += 1) {
                copyArtifacts(jobConfig['subsetJob']) {
                    buildSelector {
                        buildNumber("\$UNIT_BUILD_NUM_".concat(buildNum.toString()))
                    }
                    includePatterns('reports/**/*coverage*')
                    fingerprintArtifacts(false)
                    if (buildNum > 2) {
                         optional()
                    }
                }
            }
            // Run jenkins-report.sh which will upload coverage results to
            // codecov.
            shell("./scripts/jenkins-report.sh; " +
                  "pip freeze > \${WORKSPACE}/${jobConfig['cloneReference']}/test_root/log/pip_freeze.log")
        }
        publishers {
            archiveArtifacts {
                pattern('reports/diff_coverage_combined.html,reports/**')
                pattern('edx-platform*/test_root/log/pip_freeze.log')
                defaultExcludes()
            }
        }
    }
}

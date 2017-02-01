package platform

import org.yaml.snakeyaml.Yaml
import static org.edx.jenkins.dsl.JenkinsPublicConstants.JENKINS_PUBLIC_LOG_ROTATOR
import static org.edx.jenkins.dsl.JenkinsPublicConstants.JENKINS_PUBLIC_WORKER

// The edx-e2e-tests job is run automatically on every deployment of the edx-platform
// from the gocd pipeline.

/* stdout logger */
/* use this instead of println, because you can pass it into closures or other scripts. */
Map config = [:]
Binding bindings = getBinding()
config.putAll(bindings.getVariables())
PrintStream out = config['out']

/* Map to hold the k:v pairs parsed from the secret file */
Map mailingListMap = [:]
try {
    out.println('Parsing secret YAML file')
    String mailingListSecretContents  = new File("${MAILING_LIST_SECRET}").text
    Yaml yaml = new Yaml()
    mailingListMap = yaml.load(mailingListSecretContents)
    out.println('Successfully parsed secret YAML file')
}
catch (any) {
    out.println('Jenkins DSL: Error parsing secret YAML file')
    out.println('Exiting with error code 1')
    return 1
}

assert mailingListMap.containsKey('e2e_test_mailing_list')

job('edx-e2e-tests') {

    description('Run end-to-end tests against an instance of the edx-platform')

    authorization {
        blocksInheritance(true)
        permissionAll('edx')
    }

    logRotator JENKINS_PUBLIC_LOG_ROTATOR()
    label(JENKINS_PUBLIC_WORKER)
    // Disable concurrent builds because the environment in use is a shared
    // resource, and concurrent builds can cause spurious test results
    concurrentBuild(false)

    parameters {
        stringParam('COURSE_ORG', 'ArbiRaees', 'Organization name of the course')
        stringParam('COURSE_NUMBER', 'AR-1000', 'Course number')
        stringParam('COURSE_RUN', 'fall', 'Term in which course will run')
        stringParam('COURSE_DISPLAY_NAME', 'Manual Smoke Test Course 1 - Auto', 'Display name of the course')
    }

    scm {
        git {
            remote {
                url('https://github.com/edx/edx-e2e-tests.git')
            }
            branch('*/master')
            browser()
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

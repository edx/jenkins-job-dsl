package devops

import static org.edx.jenkins.dsl.Constants.endToEndTests
import static org.edx.jenkins.dsl.Constants.common_wrappers

endToEndTests.each { endToEndTest ->

    String jobName = "${endToEndTest.name}-end-to-end-tests"

    job(jobName){
        wrappers common_wrappers
        description(endToEndTest.description)

        parameters {
            endToEndTest.parameters.each { param ->
                stringParam(param.name, param.default, param.description)
            }
            endToEndTest.masked_parameters.each { param ->
                nonStoredPasswordParam(param.name, param.description)
            }
        }

        label(endToEndTest.workerLabel)

        scm{
            github(endToEndTest.code_dir, endToEndTest.branch)
        }

        steps {
            virtualenv {
                name(jobName)
                nature('shell')
                command readFileFromWorkspace('platform/resources/end-to-end-test.sh')
            }
        }
        publishers {
            archiveArtifacts {
                pattern('acceptance_tests/*.xml') // xunit output
                pattern('*.log') // test logs
                pattern('*.png') // test screenshots
            }
            archiveJunit('acceptance_tests/*.xml')
        }
    }
}

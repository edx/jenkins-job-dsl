// TODO Gather services: name, repo, environment variables
final serviceMap = [
    [
        'name': 'stage-ecommerce',
        'repository': 'edx/ecommerce',
    ]
]

// TODO Create job
serviceMap.each { endToEndTest ->

//    assert secretMap.containsKey('credential')

    String jobName = "${endToEndTest.name}-e2e"

    job(jobName){
//        wrappers common_wrappers
        description("End-to-end tests for ${endToEndTest.name}")

//        parameters {
//            endToEndTest.parameters.each { param ->
//                stringParam(param.name, param.default, param.description)
//            }
//            endToEndTest.masked_parameters.each { param ->
//                nonStoredPasswordParam(param.name, param.description)
//            }
//        }

        scm{
            git{
                remote{
                    github(endToEndTest.repository)
//                    credentials(secretMap['credential'])
                }
                branch('master')
            }
        }

        steps {
            virtualenv {
                name(jobName)
                nature('shell')
                // TODO: Move this file to resources/end-to-end-test.sh
                command readFileFromWorkspace('platform/resources/end-to-end-test.sh')
            }
        }
        publishers {
            archiveArtifacts {
                pattern('e2e/*.xml') // xunit output
                pattern('*.log') // test logs
                pattern('*.png') // test screenshots
            }
            archiveJunit('e2e/*.xml')
        }
    }
}

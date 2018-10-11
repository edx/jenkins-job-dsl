package platform

import static org.edx.jenkins.dsl.JenkinsPublicConstants.DEFAULT_VIEW

listView("e2e-tests") {

    description('Jobs for running the e2e tests against various platform instances')

    filterBuildQueue(true)
    filterExecutors(true)

    jobs {
        regex("(microsites-.*|edx-e2e-.*)")
    }
    columns DEFAULT_VIEW.call()

}

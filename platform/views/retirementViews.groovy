package platform

import static org.edx.jenkins.dsl.JenkinsPublicConstants.DEFAULT_VIEW

listView("user-retirement-jobs") {

    description('Jobs for running and reporting on the user retirement process')

    filterBuildQueue(true)
    filterExecutors(true)

    jobs {
        regex(".*retirement.*")
    }
    columns DEFAULT_VIEW.call()

}


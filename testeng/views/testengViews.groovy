package testeng

import static org.edx.jenkins.dsl.JenkinsPublicConstants.DEFAULT_VIEW

listView("upgrade-python-requirements") {

    description('Jobs for upgrading python requirements in edx repos')

    filterBuildQueue(true)
    filterExecutors(true)

    jobs {
        regex(".*-upgrade-python-requirements")
    }
    columns DEFAULT_VIEW.call()

}

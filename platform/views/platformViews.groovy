package platform

import static org.edx.jenkins.dsl.JenkinsPublicConstants.DEFAULT_VIEW

List<String> branchList = [ "edx-platform", // Represents all non-release testing
                            "ficus",
                            "ginkgo"
                            ]

branchList.each { branch ->

    listView("${branch}-pr-tests") {
        if (branch == "edx-platform" ) {
            description('Jobs for running tests on PRs on the edx-platform')
        }
        else {
            description("Jobs for running tests on PRs against the " +
                        "open-release/${branch}.master branch of the edx-platform")
            filterBuildQueue(true)
            filterExecutors(true)
        }

        jobs {
            regex("${branch}(-django-upgrade)?-(accessibility|bok-choy|js|lettuce|quality|python-unittests|unittests)-pr")
        }
        columns DEFAULT_VIEW.call()

    }

    listView("${branch}-master-tests") {
        if (branch == "edx-platform" ) {
            description('Jobs for running tests pushes to master branch of the edx-platform')
        }
        else {
            description("Jobs for running tests on commits pushed onto the " +
                        "open-release/${branch}.master branch of the edx-platform")
            filterBuildQueue(true)
            filterExecutors(true)
        }

        jobs {
            name('github-build-status')
            regex("${branch}(-django-upgrade)?-(accessibility|bok-choy|js|lettuce|quality|python-unittests|unittests)-master")
        }
        columns DEFAULT_VIEW.call()
    }

}

listView('django-upgrade-pr-tests') {
    description('jobs used to run tests on pull requests against various ' +
                'versions of django during the upgrade process')
    jobs {
        regex('edx-platform-django-.*-pr')
    }
    columns DEFAULT_VIEW.call()
}

listView('django-upgrade-master-tests') {
    description('jobs used to run tests on merges to master against various ' +
                'versions of django during the upgrade process')
    jobs {
        regex('edx-platform-django-.*-master')
    }
    columns DEFAULT_VIEW.call()
}

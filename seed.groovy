job('seed') {
    description('Seed job that creates other jobs.')

    parameters {
        stringParam('DSL_BRANCH', 'master', 'Branch of edx/jenkins-job-dsl to use for seeding jobs')
        stringParam('SCRIPT_PATH', null, 'Path to job to seed. Examples: platform/jobs/*.groovy OR jobs/**/*Jobs.groovy')
    }

    multiscm {
        git {
            remote {
                url 'https://github.com/edx/jenkins-job-dsl.git'
            }
            extensions {
                cleanAfterCheckout()
                pruneBranches()
            }
            branch('$DSL_BRANCH')
        }

    }

    steps {
        // NOTE: While a good idea to run tests in a production environment, testing takes time. For a quick turnaround
        // of job seeding when developing locally, change this to just 'libs' to avoid running tests.
        gradle 'clean test libs'
        dsl {
            external '$SCRIPT_PATH'
            additionalClasspath 'src/main/groovy\nlib/*.jar'
        }
    }
    publishers {
        archiveJunit 'build/test-results/**/*.xml'
    }
}

// This DSL defines the seed job that can be used to create other jobs. It ahs been adapted from
// https://github.com/sheehan/job-dsl-gradle-example. The script can be executed remotely using the Jenkins REST API.
// It assumes you have already created a credential with access to GitHub. See
// `resources/createOrUpdateCredentials.groovy` for details.
//
// Command format
//    ./gradlew rest -Dpattern=<pattern> -DbaseUrl=<baseUrl> [-Dusername=<username>] [-Dpassword=<password>]
//
// Example (without authentication)
//    ./gradlew rest -Dpattern=jobs/seed.groovy -DbaseUrl=http://localhost:8080/api

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

        // TODO If you need access to the internal repo to seed your repo, uncomment the block below.
//        git {
//            remote {
//                url 'https://github.com/edx/jenkins-job-dsl-internal.git'
//                credentials 'github'
//            }
//            extensions {
//                cleanAfterCheckout()
//                pruneBranches()
//                relativeTargetDirectory 'jenkins-job-dsl-internal'
//            }
//            branch('master')
//        }

        // TODO If seeding of your jobs relies on additional non-public repos, add them here.
//        git {
//            remote {
//                url 'https://github.com/edx-ops/ecom-secure.git'
//                credentials 'github'
//            }
//            extensions {
//                cleanAfterCheckout()
//                pruneBranches()
//                relativeTargetDirectory 'ecom-secure'
//            }
//            branch 'master'
//        }
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

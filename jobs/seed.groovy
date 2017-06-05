// If you want, you can define your seed job in the DSL and create it via the REST API.
// See https://github.com/sheehan/job-dsl-gradle-example#rest-api-runner
// ./gradlew rest -Dpattern=<pattern> -DbaseUrl=<baseUrl> [-Dusername=<username>] [-Dpassword=<password>]
// Example (without authentication): ./gradlew rest -Dpattern=jobs/seed.groovy -DbaseUrl=http://localhost:8080/api

// TODO: When testing locally, change this to your own branch.
String branch = 'master'

job('seed') {
    description('Seed job that creates other jobs.')
    scm {
        github 'edx/jenkins-job-dsl', branch
    }
    steps {
        gradle 'clean test'
        dsl {
            external '**/jobs/**/*Jobs.groovy'
            additionalClasspath 'src/main/groovy'
        }
    }
    publishers {
        archiveJunit 'build/test-results/**/*.xml'
    }
}

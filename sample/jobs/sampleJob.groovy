/*
    This sample job prints "hello world" to the console. This sample job expects
    to run with:
        - edx/jenkins-job-dsl checked out into the root of the workspace
    
    This job requires that a seed job is loaded. 
    The seed job must have a parameterized build. The first parameter is a string named DSL_BRANCH. The default job is master. The second parameter is a string named SCRIPT_PATH, the path to job to seed.

    The seed job requires that you select Multiple SCMs for Source Code Management and choose Git Repositories. The repository URL is:
        https://github.com/edx/jenkins-job-dsl.git. 
    Set Branch Specifier to: $DSL_BRANCH. 
    The job requires that you add an 'Invoke Gradle script' build step.
    Select 'Use Gradle Wrapper'. Check the 'Make gradlew executable' and 'From Root Build Script Dir'. Run the 'clean'  and 'libs' tasks.

    You must also add a 'Process Job DSLs' step. Set the 'DSL Scripts' path to: $SCRIPT_PATH.
        
    Set 'Additional classpath' to:
        jenkins-job-dsl-internal/src/main/groovy/
        jenkins-job-dsl-internal/lib/*.jar
        src/main/groovy/
        lib/*.jar
        .
    Save the seed job. Build with Parameters and set SCRIPT_PATH to be:
        sample/jobs/sampleJob.groovy
    Build the SampleJenkinsJob to run the sample job.
 */

job ('SampleJenkinsJob') {

    logRotator {
        daysToKeep(10)
        numToKeep(-1)
    }
    steps {
        shell("echo 'hello world'")
    }
}

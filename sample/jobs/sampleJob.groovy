import hudson.model.Build

job ('SampleJenkinsJob') {

    logRotator {
        daysToKeep(10)
        numToKeep(-1)
    }
    steps {
        shell('hello world')
    }
}

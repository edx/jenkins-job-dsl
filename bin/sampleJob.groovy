job ('SampleJenkinsJob') {

    logRotator {
        daysToKeep(10)
        numToKeep(-1)
    }
    steps {
        shell("echo 'hello world'")
    }
}

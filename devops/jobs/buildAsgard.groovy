job('Build-Asgard-WARs'){
    scm {
        git("https://github.com/edx/asgard", "origin/release", createTag=false)
    }

    steps {
        gradle("test")
        gradle("war")
    }
}

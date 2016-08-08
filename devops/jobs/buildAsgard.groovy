job('Build-Asgard-WARs'){
    scm {
        git("https://github.com/edx/asgard", "origin/release")
    }

    steps {
        gradle("test")
        gradle("war")
    }
}

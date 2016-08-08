job('Build-Asgard-WARs'){
    scm {
        git {
            remote {
                url("https://github.com/edx/asgard")
                branch("origin/release")
            }
            createTag(false)

        }
    }

    steps {
        grails{
            target("test")
            useWrapper(true)
        }
        grails{
            target("war")
            useWrapper(true)
        }
    }
}

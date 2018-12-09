class ConfigChecker extends RunLocalAnsiblePlaybook {
    public def post_ansible_steps() {
        virtualenv {
            name(jobName)
            nature('shell')
            command(dslFactory.readFileFromWorkspace('devops/resources/syntax-check-config.sh'))
        }
    }
}

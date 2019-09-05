class ConfigChecker extends RunLocalAnsiblePlaybook {
    public def post_ansible_steps() {
        virtualenv {
            pythonName('System-CPython-3.5')
            name(jobName)
            nature('shell')
            command(dslFactory.readFileFromWorkspace('devops/resources/syntax-check-config.sh'))
        }
    }
}

class ConfigChecker extends RunLocalAnsiblePlaybook {
    public def post_ansible_steps() {
        shell(dslFactory.readFileFromWorkspace('devops/resources/syntax-check-config.sh'))
    }
}

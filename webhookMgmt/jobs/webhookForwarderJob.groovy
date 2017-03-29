package webhookMgmt

job('forward-webhook') {

    description("Receive a webhook from Github and forward it along to ${TARGET}")
    parameters {
        stringParam('payload', 'the payload string from a Jenkins webhook')
        stringParam('taget', "${TARGET}", 'the target Jenkins to send traffic to')
    }
    wrappers {
        timestamps()
    }
    authenticationToken("${TOKEN}")
    steps {
        python {
            command(readFileFromWorkspace('webhookMgmt/resources/forward-webhook.py'))
            nature('python')
            pythonName('VENV')
        }
    }

}

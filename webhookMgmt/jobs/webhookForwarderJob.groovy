package webhookMgmt

job('forward-webhook') {

    description("Receive a webhook from Github and forward it along to ${TARGET}")
    parameters {
        stringParam('payload', 'the payload string from a Jenkins webhook')
        stringParam('target', "${TARGET}", 'the target Jenkins to send traffic to')
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

    // configure the build name setter component, as it is not supported via dsl
    configure { project ->
        project / 'builders' / 'org.jenkinsci.plugins.buildnameupdater.BuildNameUpdater' {
            buildName 'custom_description';
            macroTemplate '#${BUILD_NUMBER}';
            fromFile true;
            fromMacro false;
            macroFirst false;
        }
    }

}

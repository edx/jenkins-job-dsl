package mobileApp

import org.yaml.snakeyaml.Yaml
import static org.edx.jenkins.dsl.JenkinsPublicConstants.JENKINS_PUBLIC_LOG_ROTATOR

/* stdout logger */
Map config = [:]
Binding bindings = getBinding()
config.putAll(bindings.getVariables())
PrintStream out = config['out']

/* Map to hold the k:v pairs parsed from the secret file */
Map ghprbMap = [:]
try {
    out.println('Parsing secret YAML file')
    String ghprbConfigContents = new File("${GHPRB_SECRET}").text
    Yaml yaml = new Yaml()
    ghprbMap = yaml.load(ghprbConfigContents)
    out.println('Successfully parsed secret YAML file')
}
catch (any) {
    out.println('Jenkins DSL: Error parsing secret YAML file')
    out.println('Exiting with error code 1')
    return 1
}

pipelineJob('edx-app-android-build') {

    description('Build the edX Android app and archive it on Jenkins')

    authorization {
        blocksInheritance(true)
        // Tools team manages this job
        permissionAll('edx*testeng')
        // Android developers can view and trigger this job
        List developerTeams = ['edx*edx-mobile-push']
        developerTeams.each { team ->
            permission('hudson.model.Item.Read', team)
            permission('hudson.model.Item.Discover', team)
            permission('hudson.model.Item.Workspace', team)
            permission('hudson.model.Item.Build', team)
            permission('hudson.model.Item.Cancel', team)
        }
    }

    definition {

        logRotator JENKINS_PUBLIC_LOG_ROTATOR(7)

        triggers {
            githubPullRequest {
                admins(ghprbMap['admin'])
                useGitHubHooks(true)
                triggerPhrase('jenkins run tests')
                onlyTriggerPhrase(false)
                userWhitelist(ghprbMap['userWhiteList'])
                orgWhitelist(ghprbMap['orgWhiteList'])
                extensions {
                    commitStatus {
                        context('jenkins/build')
                    }
                }
            }
        }

        cpsScm {
            scm {
                git {
                    remote {
                        credentials('jenkins-worker')
                        github('openedx/edx-app-android', 'https', 'github.com')
                        refspec('+refs/pull/${ghprbPullId}/*:refs/remotes/origin/pr/${ghprbPullId}/*')
                    }
                    branch('\${sha1}')
                }
            }
            scriptPath('Jenkinsfile')
        }
    }
}

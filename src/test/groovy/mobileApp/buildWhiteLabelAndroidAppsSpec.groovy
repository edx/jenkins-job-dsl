package mobileApp

import groovy.util.slurpersupport.GPathResult
import groovy.util.slurpersupport.Node
import javaposse.jobdsl.plugin.JenkinsJobManagement
import javaposse.jobdsl.dsl.GeneratedItems
import javaposse.jobdsl.dsl.GeneratedJob
import javaposse.jobdsl.dsl.DslScriptLoader
import javaposse.jobdsl.dsl.JobManagement
import org.junit.ClassRule
import org.jvnet.hudson.test.JenkinsRule
import spock.lang.Shared
import spock.lang.Specification
import spock.lang.Unroll

class buildWhiteLabelAndroidAppsSpec extends Specification {

    @Shared @ClassRule
    JenkinsRule jenkinsRule = new JenkinsRule()

    @Shared DslScriptLoader loader
    @Shared JobManagement jm

    @Shared File dslScript = new File('mobileApp/jobs/buildWhiteLabelAndroidApps.groovy')
    @Shared String baseSecretPath = 'src/test/resources/mobileApp/secrets'
    @Shared String secretVar = 'BUILD_ANDROID_SECRET'

    @Shared List pList = [ 'com.cloudbees.plugins.credentials.CredentialsProvider.Delete:edx',
                                'com.cloudbees.plugins.credentials.CredentialsProvider.ManageDomains:edx',
                                'hudson.model.Item.Read:edx', 'hudson.model.Item.Configure:edx',
                                'hudson.model.Item.Workspace:edx', 'hudson.model.Run.Delete:edx',
                                'hudson.model.Item.Discover:edx', 'hudson.scm.SCM.Tag:edx',
                                'com.cloudbees.plugins.credentials.CredentialsProvider.View:edx',
                                'hudson.model.Item.Build:edx', 'hudson.model.Item.Move:edx',
                                'com.cloudbees.plugins.credentials.CredentialsProvider.Create:edx',
                                'hudson.model.Item.Cancel:edx', 'hudson.model.Item.Delete:edx',
                                'hudson.model.Run.Update:edx',
                                'com.cloudbees.plugins.credentials.CredentialsProvider.Update:edx' ]

    /*
    * Helper function: loadSecret
    * return a JenkinsJobManagement object containing a mapping of secret variables to secret values
    */
    JenkinsJobManagement loadSecret(secretKey, secretValue) {
        Map<String,String> envVars = [:]
        envVars.put(secretKey, secretValue)
        JenkinsJobManagement jjm = new JenkinsJobManagement(System.out, envVars, new File('.'))
        jjm
    }

    /*
    * TODO: condense this into a shared helper
    * Helper function: loadSecrets
    * return a JenkinsJobManagement object containing a mapping of secret variables to secret values
    */
    JenkinsJobManagement loadSecrets(envVars) {
        JenkinsJobManagement jjm = new JenkinsJobManagement(System.out, envVars, new File('.'))
        jjm
    }

    /**
    * Using an evironment variable that points to a non-existent secret file, ensure that no
    * jobs are created.
    **/
    void 'test non-existent secret file is handled correctly'() {

        setup:
        String secretPath = baseSecretPath + '/non-existent-file.yml'
        jm =  loadSecret(secretVar, secretPath)
        loader = new DslScriptLoader(jm)

        when:
        GeneratedItems generatedItems = loader.runScript(dslScript.text)

        then:
        generatedItems.jobs.size() == 0
    }

    /**
    * Using an invalid yaml secret, ensure that the appropriate exceptions are thrown, and
    * that no jobs are created.
    **/
    void 'test invalid yaml is handled correctly'() {

        setup:
        String secretPath = baseSecretPath + '/corrupt-secret.yml'
        jm =  loadSecret(secretVar, secretPath)
        loader = new DslScriptLoader(jm)

        when:
        GeneratedItems generatedItems = loader.runScript(dslScript.text)

        then:
        generatedItems.jobs.size() == 0
    }

    /**
    * Run the DSL script and verify that no exceptions were thrown by the dsl script runner
    * and that the correct number of jobs was created and that each generated job has the 
    * correct name
    **/
    void 'test correct jobs are created'() {

        setup:
        String secretPath = baseSecretPath + '/build-android-secret.yml'
        jm =  loadSecret(secretVar, secretPath)
        loader = new DslScriptLoader(jm)

        when:
        loader.runScript(dslScript.text)
        GeneratedItems generatedItems = loader.runScript(dslScript.text)
        GeneratedJob job1 = new GeneratedJob(null, 'build-first-app')
        GeneratedJob job2 = new GeneratedJob(null, 'build-second-app')

        then:
        noExceptionThrown()
        generatedItems.jobs.size() == 2
        generatedItems.jobs.contains(job1)
        generatedItems.jobs.contains(job2)

    }

    /**
    * Run DSL jobs and verify that the output jobs are private to the organization
    **/
    @Unroll("Test privacy in #job")
    void 'test jobs are private'() {

        setup:
        String secretPath = baseSecretPath + '/build-android-secret.yml'
        jm =  loadSecret(secretVar, secretPath)
        loader = new DslScriptLoader(jm)

        when:
        loader.runScript(dslScript.text)
        GPathResult project = new XmlSlurper().parseText(jm.getConfig(job))

        then:
        Node scm = project.childNodes().find { it.name == 'scm' }
        Node urc = scm.childNodes().find { it.name == 'userRemoteConfigs' }
        Node giturc = urc.childNodes().find { it.name == 'hudson.plugins.git.UserRemoteConfig' }
        giturc.childNodes().any { it.name == "url" && it.text() == gitRepo }
        if (!pub) {
            giturc.childNodes().any { it.name == 'credentialsId' && it.text() ==  cred }
            Node prop = project.childNodes().find { it.name == 'properties' }
            Node privacyBlock = prop.childNodes().find { it.name == 'hudson.security.AuthorizationMatrixProperty' }
            privacyBlock.childNodes().any { it.name == 'blocksInheritance' && it.text() == 'true' }
            ArrayList<Node> permissions = privacyBlock.childNodes().findAll { it.name == 'permission' } 
            permissions.each { pList.contains(it.text()) }
        }

        where:
        job                | pub   | gitRepo                                   | cred    | permissions
        'build-first-app'  | true  | 'https://github.com/org/android-app1.git' | 'user1' | _
        'build-second-app' | false | 'https://github.com/org/android-app2.git' | 'user2' | pList

    }

    /**
    * Run DSL jobs and verify that the hockyApp closure is configured correctly, as it
    * is not part of the Jenkins job DSL
    **/
    void 'test hockeyApp configuration is created correctly'() {

        setup:
        String secretPath = baseSecretPath + '/build-android-secret.yml'
        jm = loadSecret(secretVar, secretPath)
        loader = new DslScriptLoader(jm)

        when:
        loader.runScript(dslScript.text)
        GPathResult project = new XmlSlurper().parseText(jm.getConfig('build-first-app'))

        then:
        Node publishers = project.childNodes().find { it.name == 'publishers' }
        Node flex = publishers.childNodes().find { it.name == 'org.jenkins__ci.plugins.flexible__publish.FlexiblePublisher' }
        Node subPublishers =  flex.childNodes().find { it.name == 'publishers' }
        Node condPublisher = subPublishers.childNodes().find { it.name == 'org.jenkins__ci.plugins.flexible__publish.ConditionalPublisher' }
        Node pubList = condPublisher.childNodes().find { it.name == 'publisherList' }
        Node recorder = pubList.childNodes().find { it.name == 'hockeyapp.HockeyappRecorder' }
        Node applications = recorder.childNodes().find { it.name == 'applications' }
        Node hockeyApp = applications.childNodes().find { it.name == 'hockeyapp.HockeyappApplication' }
        hockeyApp.childNodes().any { it.name == 'apiToken' && it.text() == '123'}
        hockeyApp.childNodes().any { it.name == 'filePath' && it.text() == 'artifacts/*.apk' }

    }

}

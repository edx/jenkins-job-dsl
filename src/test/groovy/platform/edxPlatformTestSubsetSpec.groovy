package platform

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

class edxPlatformTestSubsetSpec extends Specification {

    @Shared
    @ClassRule
    JenkinsRule jenkinsRule = new JenkinsRule()

    def @Shared DslScriptLoader loader
    def @Shared JobManagement jm

    // The DSL script under test
    def @Shared File dslScript = new File('platform/jobs/edxPlatformTestSubset.groovy')
    def @Shared String baseJobName = "edx-platform-test-subset"
    def @Shared String baseSecretPath = "src/test/resources/platform/secrets"
    // Secret variable used in DSL script
    def @Shared String secretVar = "EDX_PLATFORM_TEST_SUBSET_SECRET"
    
    def @Shared List pList = [ 'com.cloudbees.plugins.credentials.CredentialsProvider.Delete:edx',  
                                'com.cloudbees.plugins.credentials.CredentialsProvider.ManageDomains:edx', 'hudson.model.Item.Read:edx', 
                                'hudson.model.Item.Configure:edx', 'hudson.model.Item.Workspace:edx', 'hudson.model.Run.Delete:edx', 
                                'hudson.model.Item.Discover:edx', 'com.cloudbees.plugins.credentials.CredentialsProvider.View:edx', 
                                'hudson.model.Item.Build:edx', 'com.cloudbees.plugins.credentials.CredentialsProvider.Create:edx', 
                                'hudson.model.Item.Cancel:edx', 'hudson.model.Item.Delete:edx', 'hudson.model.Run.Update:edx', 
                                'com.cloudbees.plugins.credentials.CredentialsProvider.Update:edx' ]

    /*
    * Helper function: loadSecret
    * return a JenkinsJobManagement object containing a mapping of secret variables to secret values
    */
    JenkinsJobManagement loadSecret(secretKey, secretValue) {
        Map<String,String> envVars = new HashMap<String,String>()
        envVars.put(secretKey, secretValue)
        JenkinsJobManagement jjm = new JenkinsJobManagement(System.out, envVars, new File('.'))
        return jjm
    }


    /**
    * Using an evironment variable that points to a non-existent secret file, ensure that no
    * jobs are created.
    **/
    void 'test non-existent secret file is handled correctly'() {

        setup:
        String secretPath = baseSecretPath + "/non-existent-file.yml"
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
        String secretPath = baseSecretPath + "/corrupt-secret.yml"
        jm =  loadSecret(secretVar, secretPath)
        loader = new DslScriptLoader(jm)

        when:
        GeneratedItems generatedItems = loader.runScript(dslScript.text)

        then:
        generatedItems.jobs.size() == 0
    }

    /**
    * Using a valid, but incomplete yaml (missing a key:value pair) verify that an assertionError is
    * thrown.
    **/
    void 'test incomplete secret is handled correctly'() {

        setup:
        String secretPath = baseSecretPath + "/incomplete-secret.yml"
        jm =  loadSecret(secretVar, secretPath)
        loader = new DslScriptLoader(jm)

        when:
        GeneratedItems generatedItems = loader.runScript(dslScript.text)

        then:
        thrown(AssertionError)

    }

    /**
    * Run the DSL script and verify that no exceptions were thrown by the dslScriptRunner
    **/
    void 'test no exceptions are thrown'() {

        setup:
        String secretPath = baseSecretPath + "/edx-platform-test-subset-secret.yml"
        jm =  loadSecret(secretVar, secretPath)
        loader = new DslScriptLoader(jm)

        when:
        loader.runScript(dslScript.text)

        then:
        noExceptionThrown()  

    }

    /**
    * Run the DSL script and verify that the correct number of jobs was created and that
    * each generated job has the correct name
    **/
    void 'test correct jobs are created'() {

        setup:
        String secretPath = baseSecretPath + "/edx-platform-test-subset-secret.yml"
        jm =  loadSecret(secretVar, secretPath)
        loader = new DslScriptLoader(jm)

        when:
        loader.runScript(dslScript.text)
        GeneratedItems generatedItems = loader.runScript(dslScript.text)
        GeneratedJob job1 = new GeneratedJob(null, baseJobName)
        GeneratedJob job2 = new GeneratedJob(null, baseJobName + '_2')

        then:
        generatedItems.jobs.size() == 2
        generatedItems.jobs.contains(job1)
        generatedItems.jobs.contains(job2)

    }
    
    /**
    * Run the DSL script and verify that the values from the secret file created the
    * correct XML structures in the generated jobs
    **/
    @Unroll('JOB : #job')
    void 'test secret creates correct xml'() {

        setup:
        String secretPath = baseSecretPath + "/edx-platform-test-subset-secret.yml"
        jm =  loadSecret(secretVar, secretPath)
        loader = new DslScriptLoader(jm)

        when:
        loader.runScript(dslScript.text)
        String conf = jm.getConfig(job)
        GPathResult project = new XmlSlurper().parseText(jm.getConfig(job))

        then:
        Node scm = project.childNodes().find { it.name == "scm" }
        Node urc = scm.childNodes().find { it.name == "userRemoteConfigs" }
        Node giturc = urc.childNodes().find { it.name == "hudson.plugins.git.UserRemoteConfig" }
        giturc.childNodes().any { it.name == "url" && it.text() == url }
        if (!open) {
            giturc.childNodes().any { it.name == "credentialsId" && it.text() ==  cred }
        }
        Node ext = scm.childNodes().find { it.name == "extensions" }
        Node cloneOption = ext.childNodes().find { it.name == "hudson.plugins.git.extensions.impl.CloneOption" }
        cloneOption.childNodes().any { it.name == "reference" && it.text() == "\$HOME/${clone}"}

        where:
        job                                 | open  | url                                           | cred          | clone
        "edx-platform-test-subset"          | true  | "https://github.com/edx/edx-platform.git"     | false         | "edx-platform-clone/.git"
        "edx-platform-test-subset_2"        | false | "ssh://github.com/edx/edx-platform-2.git"     | "password"    | "edx-platform-2-clone/.git"


    }

    /**
    * Run the DSL script and verify that the project security settings for the generated jobs
    * are set correctly
    **/
    @Unroll('JOB : #job')
    void 'test security settings'() {

        setup:
        String secretPath = baseSecretPath + "/edx-platform-test-subset-secret.yml"
        jm =  loadSecret(secretVar, secretPath)
        loader = new DslScriptLoader(jm)

        when:
        loader.runScript(dslScript.text)
        GPathResult project = new XmlSlurper().parseText(jm.getConfig(job))

        then:
        Node properties = project.childNodes().find { it.name == "properties" }
        if (!open) {
            Node privacyBlock = properties.childNodes().find { it.name == 'hudson.security.AuthorizationMatrixProperty' }
            privacyBlock.childNodes().any { it.name == "blocksInheritance" && it.text() == block.toString() }
            privacyBlock.childNodes().each {
                if (it.name == 'permissions') {
                    permissions.contains(it.text())
                }
            }
        }
        else {
            !properties.childNodes().any { it.name == 'hudson.security.AuthorizationMatrixProperty' }
        }

        where:

        job                                 | open  | block | permissions 
        "edx-platform-test-subset"          | true  | false | _
        "edx-platform-test-subset_2"        | false | true  | pList

    }

}

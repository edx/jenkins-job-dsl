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
    def @Shared String baseJobName = 'edx-platform-test-subset'

    def @Shared List pList = [ 'com.cloudbees.plugins.credentials.CredentialsProvider.Delete:edx',
                                'com.cloudbees.plugins.credentials.CredentialsProvider.ManageDomains:edx',
                                'hudson.model.Item.Read:edx','hudson.model.Item.Configure:edx',
                                'hudson.model.Item.Workspace:edx', 'hudson.model.Run.Delete:edx',
                                'com.cloudbees.plugins.credentials.CredentialsProvider.View:edx',
                                'com.cloudbees.plugins.credentials.CredentialsProvider.Create:edx',
                                'hudson.model.Item.Discover:edx', 'hudson.model.Item.Build:edx',
                                'hudson.model.Item.Cancel:edx', 'hudson.model.Item.Delete:edx',
                                'com.cloudbees.plugins.credentials.CredentialsProvider.Update:edx',
                                'hudson.model.Run.Update:edx' ]

    /**
    * Run the DSL script and verify that no exceptions were thrown by the dslScriptRunner
    **/
    void 'test no exceptions are thrown'() {

        setup:
        JenkinsJobManagement jm = new JenkinsJobManagement(System.out, [:], new File('.'))
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
        JenkinsJobManagement jm = new JenkinsJobManagement(System.out, [:], new File('.'))
        loader = new DslScriptLoader(jm)

        when:
        loader.runScript(dslScript.text)
        GeneratedItems generatedItems = loader.runScript(dslScript.text)
        GeneratedJob job1 = new GeneratedJob(null, baseJobName)
        GeneratedJob job2 = new GeneratedJob(null, baseJobName + '_private')

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
        JenkinsJobManagement jm = new JenkinsJobManagement(System.out, [:], new File('.'))
        loader = new DslScriptLoader(jm)

        when:
        loader.runScript(dslScript.text)
        String conf = jm.getConfig(job)
        GPathResult project = new XmlSlurper().parseText(jm.getConfig(job))

        then:
        Node scm = project.childNodes().find { it.name == 'scm' }
        Node urc = scm.childNodes().find { it.name == 'userRemoteConfigs' }
        Node giturc = urc.childNodes().find { it.name == 'hudson.plugins.git.UserRemoteConfig' }
        giturc.childNodes().any { it.name == 'url' && it.text() == "${protocol}${url}.git" }
        Node ext = scm.childNodes().find { it.name == 'extensions' }
        Node cloneOption = ext.childNodes().find { it.name == 'hudson.plugins.git.extensions.impl.CloneOption' }
        cloneOption.childNodes().any { it.name == 'reference' && it.text() == "\$HOME/${clone}"}

        where:
        job                                | open  | protocol          | url                        | clone
        'edx-platform-test-subset'         | true  | 'git@github.com:' | 'edx/edx-platform'         | 'edx-platform-clone/.git'
        'edx-platform-test-subset_private' | false | 'git@github.com:' | 'edx/edx-platform-private' | 'edx-platform-private-clone/.git'


    }

    /**
    * Run the DSL script and verify that the project security settings for the generated jobs
    * are set correctly
    **/
    @Unroll('JOB : #job')
    void 'test security settings'() {

        setup:
        JenkinsJobManagement jm = new JenkinsJobManagement(System.out, [:], new File('.'))
        loader = new DslScriptLoader(jm)

        when:
        loader.runScript(dslScript.text)
        GPathResult project = new XmlSlurper().parseText(jm.getConfig(job))

        then:
        Node properties = project.childNodes().find { it.name == 'properties' }
        if (!open) {
            Node privacyBlock = properties.childNodes().find {
                it.name == 'hudson.security.AuthorizationMatrixProperty'
            }
            privacyBlock.childNodes().any { it.name == 'blocksInheritance' && it.text() == block.toString() }
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
        'edx-platform-test-subset'          | true  | false | _
        'edx-platform-test-subset_private'  | false | true  | pList

    }


}

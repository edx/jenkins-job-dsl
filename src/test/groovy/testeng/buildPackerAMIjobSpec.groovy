package testeng

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

class buildPackerAMIjobSpec extends Specification {

    @Shared @ClassRule
    JenkinsRule jenkinsRule = new JenkinsRule()

    @Shared DslScriptLoader loader
    @Shared JobManagement jm

    @Shared File dslScript = new File('testeng/jobs/buildPackerAMIjob.groovy')

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
    * Run DSL jobs and verify that the output jobs are private to the organization
    **/
    void 'test jobs are private'() {

        setup:
        JenkinsJobManagement jm = new JenkinsJobManagement(System.out, [:], new File('.'))
        loader = new DslScriptLoader(jm)

        when:
        loader.runScript(dslScript.text)
        GPathResult project = new XmlSlurper().parseText(jm.getConfig('build-packer-ami'))

        then:
        Node prop = project.childNodes().find { it.name == 'properties' }
        Node privacyBlock = prop.childNodes().find { it.name == 'hudson.security.AuthorizationMatrixProperty' }
        privacyBlock.childNodes().any { it.name == 'blocksInheritance' && it.text() == 'true' }
        ArrayList<Node> permissions = privacyBlock.childNodes().findAll { it.name == 'permission' }
        permissions.each { pList.contains(it.text()) }

    }

}

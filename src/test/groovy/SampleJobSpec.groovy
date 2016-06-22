
import groovy.util.slurpersupport.GPathResult
import javaposse.jobdsl.dsl.DslScriptLoader
import javaposse.jobdsl.dsl.JobManagement
import javaposse.jobdsl.plugin.JenkinsJobManagement
import javaposse.jobdsl.dsl.GeneratedItems
import javaposse.jobdsl.dsl.GeneratedJob
import org.junit.ClassRule
import org.jvnet.hudson.test.JenkinsRule
import spock.lang.Shared
import spock.lang.Specification
import spock.lang.Unroll

/**
 * Basic POC tests for the sample dsl script sample/jobs/sampleJob.groovy
 */
class SampleJobSpec extends Specification {

    @Shared
    @ClassRule
    JenkinsRule jenkinsRule = new JenkinsRule()

    @Unroll
    void 'test no exceptions thrown when dsl is run'() {

        given:
        JobManagement jm = new JenkinsJobManagement(System.out, [:], new File('.'))
        DslScriptLoader loader = new DslScriptLoader(jm)
        File dslScript = new File('sample/jobs/sampleJob.groovy')

        when:
        loader.runScript(dslScript.text)

        then:
        noExceptionThrown()

    }

    @Unroll
    void 'test name of generated job'() {

        given:
        JobManagement jm = new JenkinsJobManagement(System.out, [:], new File('.'))
        DslScriptLoader loader = new DslScriptLoader(jm)
        File dslScript = new File('sample/jobs/sampleJob.groovy')

        when:
        GeneratedItems generatedItems = loader.runScript(dslScript.text)

        then:
        GeneratedJob gj = new GeneratedJob(null, 'SampleJenkinsJob')
        generatedItems.jobs.contains(gj)

    }

    @Unroll
    void 'test particular text content in generated xml'() {

        given:
        JobManagement jm = new JenkinsJobManagement(System.out, [:], new File('.'))
        DslScriptLoader loader = new DslScriptLoader(jm)
        File dslScript = new File('sample/jobs/sampleJob.groovy')

        when:
        loader.runScript(dslScript.text)
        String config = jm.getConfig('SampleJenkinsJob')

        then:
        config.contains('hello world')

    }

    @Unroll
    void 'test particular xml structure within generated xml'() {

        given:
        JobManagement jm = new JenkinsJobManagement(System.out, [:], new File('.'))
        DslScriptLoader loader = new DslScriptLoader(jm)
        File dslScript = new File('sample/jobs/sampleJob.groovy')

        when:
        loader.runScript(dslScript.text)
        GPathResult project = new XmlSlurper().parseText(jm.getConfig('SampleJenkinsJob'))

        then:
        GPathResult logRotatorBlock = project.childNodes().find { it.name == 'logRotator' }
        logRotatorBlock.childNodes().any { it.name == 'daysToKeep' && it.text() == '10' }

    }

}

package sample

import groovy.util.slurpersupport.GPathResult
import groovy.util.slurpersupport.Node
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
 * Basic POC tests for the sample dsl script sample/jobs/sampleJob.groovy.
 * I have added a bit more information to help people become comfortable with
 * Spock specs
 */
class SampleJobSpec extends Specification {

    @Shared
    @ClassRule
    JenkinsRule jenkinsRule = new JenkinsRule()

    // Instance variables annotated with @Shared can be accessed in the test methods
    def @Shared DslScriptLoader loader
    def @Shared JobManagement jm
    def @Shared File dslScript = new File('sample/jobs/sampleJob.groovy')

    // setupSpec method will be run when the class is loaded (only once), and set up
    // basic components needed to run the DSL scripts
    def setupSpec() {
        jm = new JenkinsJobManagement(System.out, [:], new File('.'))
        loader = new DslScriptLoader(jm)
    }

    void 'test no exceptions thrown when dsl is run'() {

        when:
        loader.runScript(dslScript.text)

        then:
        // Spock does not require an explicit "assert" keyword, so this is a test.
        noExceptionThrown()

    }

    void 'test name of generated job'() {

        when:
        GeneratedItems generatedItems = loader.runScript(dslScript.text)

        then:
        // Ensure that the DslScriptLoader has created a Job instance with the expected name 'SampleJenkinsJob'
        GeneratedJob gj = new GeneratedJob(null, 'SampleJenkinsJob')
        generatedItems.jobs.contains(gj)

    }

    void 'test particular text content in generated xml'() {

        when:
        loader.runScript(dslScript.text)
        // Get the actual XML created via running the DSL job
        String config = jm.getConfig('SampleJenkinsJob')

        then:
        config.contains('hello world')

    }

    
    // The following two tests will expect the following structure be present in the XML generated
    // from runnning the dsl script. (Commenting differently for IDE related aesthetics)
    //
    //    <logRotator>
    //       <daysToKeep>10</daysToKeep>
    //       <numToKeep>-1</numToKeep>
    //       <artifactDaysToKeep>-1</artifactDaysToKeep>
    //       <artifactNumToKeep>-1</artifactNumToKeep>
    //    </logRotator>
    void 'test particular xml structure within generated xml'() {

        when:
        loader.runScript(dslScript.text)
        // Parse the XML output of running the DSL into a GPath (easy to navigate structure)
        GPathResult project = new XmlSlurper().parseText(jm.getConfig('SampleJenkinsJob'))

        then:
        // Get the logRotator xml structure and verify individual components
        Node logRotatorBlock = project.childNodes().find { it.name == 'logRotator' }
        logRotatorBlock.childNodes().any { it.name == 'daysToKeep' && it.text() == '10' }

    }

    // The unroll annotation will treat each iteration of this test (based on the data table
    // in the 'where' clause) as a separate test, and name it according to the pattern in
    // parentheses
    @Unroll('test xml contains node #nodeName with value #nodeValue')
    void 'test particular xml structure within generated xml- with a data table'() {

        when:
        loader.runScript(dslScript.text)
        GPathResult project = new XmlSlurper().parseText(jm.getConfig('SampleJenkinsJob'))

        then:
        Node logRotatorBlock = project.childNodes().find { it.name == 'logRotator' }
        logRotatorBlock.childNodes().any { it.name == nodeName && it.text() == nodeValue }
        
        // This feature method will be run for every row within the following data table,
        // swapping the column names in as variables
        where:
        nodeName             | nodeValue 
        'daysToKeep'         | '10'
        'numToKeep'          | '-1'
        'artifactDaysToKeep' | '-1'
        'artifactNumToKeep'  | '-1'
    }
    
}

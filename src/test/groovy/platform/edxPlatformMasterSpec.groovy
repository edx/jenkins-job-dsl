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


class edxPlatformMasterJobSpec extends Specification {

    @Shared
    @ClassRule
    JenkinsRule jenkinsRule = new JenkinsRule()

    def @Shared DslScriptLoader loader
    def @Shared JobManagement jm

    /**
    * Seed a DSL script and verify that the correct number of jobs are created, without throwing
    * any exceptions
    **/
    @Unroll("test seeding #dslFile")
    void 'test secret creates correct xml'() {

        setup:
        JenkinsJobManagement jm = new JenkinsJobManagement(System.out, [:], new File('.'))
        jm.setFailOnMissingPlugin(false)
        loader = new DslScriptLoader(jm)

        when:
        File dslScriptPath = new File("platform/jobs/${dslFile}")
        GeneratedItems generatedItems = loader.runScript(dslScriptPath.text)

        then:
        generatedItems.jobs.size() == numJobs
        noExceptionThrown()

        where:
        dslFile                                   | numJobs
        'edxPlatformAccessibilityMaster.groovy'   | 5
        'edxPlatformBokChoyMaster.groovy'         | 5
        'edxPlatformJsMaster.groovy'              | 5
        'edxPlatformLettuceMaster.groovy'         | 5
        'edxPlatformPythonUnitTestsMaster.groovy' | 5
        'edxPlatformQualityMaster.groovy'         | 3
        'edxPlatformQualityDiff.groovy'           | 2
        'edxPlatformUnitCoverage.groovy'          | 2
    }
}

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

class edxPlatformPrJobSpec extends Specification {

    @Shared
    @ClassRule
    JenkinsRule jenkinsRule = new JenkinsRule()

    def @Shared DslScriptLoader loader
    def @Shared JobManagement jm

    /*
    * Helper function: loadSecrets
    * return a JenkinsJobManagement object containing a mapping of secret variables to secret values
    */
    JenkinsJobManagement loadSecrets(envVars) {
        JenkinsJobManagement jjm = new JenkinsJobManagement(System.out, envVars, new File('.'))
        return jjm
    }

    /**
    * Seed a DSL script and verify that the correct number of jobs are created, without throwing
    * any exceptions
    **/
    @Unroll("test")
    void 'test secret creates correct xml'() {

        setup:
        HashMap<String, String> envVars = new HashMap<String, String>()
        envVars.put('GHPRB_SECRET', 'src/test/resources/platform/secrets/ghprb-config-secret.yml')
        jm = loadSecrets(envVars)
        loader = new DslScriptLoader(jm)

        when:
        File dslScriptPath = new File("platform/jobs/${dslFile}")

        GeneratedItems generatedItems = loader.runScript(dslScriptPath.text)

        then:
        generatedItems.jobs.size() == numJobs
        noExceptionThrown()

        where:
        dslFile                               | numJobs
        'edxPlatformAccessibilityPr.groovy'   | 6
        'edxPlatformBokChoyPr.groovy'         | 9
        'edxPlatformJsPr.groovy'              | 6
        'edxPlatformLettucePr.groovy'         | 9
        'edxPlatformPythonUnitTestsPr.groovy' | 7
        'edxPlatformQualityPr.groovy'         | 6
        'edxPlatformUnitCoverage.groovy'      | 2
    }
}

package devops

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


class UserRetirementArchiverJobSpec extends Specification {

    @Shared
    @ClassRule
    JenkinsRule jenkinsRule = new JenkinsRule()

    def @Shared DslScriptLoader loader
    def @Shared JobManagement jm

    /**
    * Seed a DSL script and verify that the job is created, without throwing
    * any exceptions
    **/
    void 'test seeding dsl creates a job'() {

        setup:
        JenkinsJobManagement jm = new JenkinsJobManagement(System.out, [:], new File('.'))
        loader = new DslScriptLoader(jm)

        when:
        File dslTestSeederScriptPath = new File(
            "src/test/resources/devops/seeders/UserRetirementArchiverTestSeeder.groovy"
        )
        GeneratedItems generatedItems = loader.runScript(dslTestSeederScriptPath.text)

        then:
        noExceptionThrown()
        generatedItems.jobs.size() == 1
    }
}

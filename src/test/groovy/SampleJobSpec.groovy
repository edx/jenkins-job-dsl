
import groovy.io.FileType

import javaposse.jobdsl.dsl.DslScriptLoader
import javaposse.jobdsl.dsl.JobManagement
import javaposse.jobdsl.plugin.JenkinsJobManagement
import org.junit.ClassRule
import org.jvnet.hudson.test.JenkinsRule
import spock.lang.Shared
import spock.lang.Specification
import spock.lang.Unroll
import spock.lang.MockingApi
import hudson.EnvVars
import hudson.model.AbstractBuild
import hudson.model.AbstractProject
import hudson.model.AbstractCIBase
import hudson.model.Hudson
import java.io.PrintStream
import hudson.model.Build
import hudson.model.FreeStyleProject
/**
 * Tests that all dsl scripts in the jobs directory will compile.
 */
class JobScriptsSpec extends Specification {
/*    
    static class FakeP extends AbstractProject {
        public String name;

        public FakeP() {
            this.name = "faked_name";
        }

        @Override
        public boolean isFingerprintConfigured() {}
    }
    
*/
    @Shared
    @ClassRule
    JenkinsRule jenkinsRule = new JenkinsRule()

    @Unroll
    void 'test script #file.name'(File file, File filex, String word) {
        given:
        JobManagement jm = new JenkinsJobManagement(System.out, [:], new File('.'))
        //AbstractBuild build = GroovyMock(AbstractBuild)
        //JenkinsJobManagement jm = new JenkinsJobManagement(System.out, new EnvVars(), build)
        //JobManagement jm = GroovyMock(JenkinsJobManagement, [System.out, [:], new File('.')])
        //AbstractCIBase acib = new AbstractCIBase()
        //Hudson j = new Hudson(new File('/tmp/tmp-jenkins'),  javax.servlet.ServletContext context)
        //AbstractProject ap = GroovyMock(constructorArgs : [ j, 'hey world' ])
        //AbstractBuild ab = GroovyMock(constructorArgs : [ap])
        //Map<String, String> envVars = [:]
        //File f = GroovyMock()
        //PrintStream ps = new PrintStream(f)
        //AbstractBuild ab = GroovyMock(Build)
        //Build ab = GroovyMock(constructorArgs : [])
        //JenkinsJobManagement jm = GroovyMock(constructorArgs : [ps, envVars, new File('workspacer')])
       // FakeP fakep = new FakeP()
       // Fake fake = new Fake(fakp)
        //AbstractProject ap = new AbstractProject()
        //AbstractBuild ab = new AbstractBuild()
        //JobManagement jm = new JenkinsJobManagement(System.out, [:], fake)
        //JenkinsJobManagement jm = GroovyMock()

        when:
        new DslScriptLoader(jm).runScript(file.text)

        then:
        noExceptionThrown()
        "Hey there" == word

        where:
        file << globFiles
        filex << bobFiles
        word = myWord
    }

    static List<File> getGlobFiles() {
        List<File> files = []
        new File('sample/jobs').eachFileRecurse(FileType.FILES) {
            if (it.name.endsWith('.groovy')) {
                files << it
            }
        }
        files
    }
    
    static List<File> getBobFiles() {
        List<File> files = []
        new File('sample/jobs').eachFileRecurse(FileType.FILES) {
            if (it.name.endsWith('.groovy')) {
                files << it
            }
        }
        files
    }


    static String getMyWord() {
        String w = "Hey there!"
    }

}


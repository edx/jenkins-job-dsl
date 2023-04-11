package analytics
import static org.edx.jenkins.dsl.AnalyticsConstants.common_triggers
import static org.edx.jenkins.dsl.AnalyticsConstants.common_log_rotator
import static org.edx.jenkins.dsl.AnalyticsConstants.common_publishers

class TestHelloWorld {
    public static def job = { dslFactory, allVars ->
        dslFactory.job('test-hello-world') {
            logRotator common_log_rotator(allVars)
            triggers common_triggers(allVars)
            wrappers {
                timestamps()
            }
            steps {
                shell(dslFactory.readFileFromWorkspace('dataeng/resources/test-hello-world.sh'))
            }
        }
    }
}

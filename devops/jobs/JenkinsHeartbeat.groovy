/*
    Job to check if Jenkins is successfully running
*/
package devops.jobs
import javaposse.jobdsl.dsl.DslFactory
import static org.edx.jenkins.dsl.Constants.common_logrotator
import static org.edx.jenkins.dsl.Constants.common_wrappers

class JenkinsHeartbeat{
    public static job( DslFactory dslFactory, Map extraVars){
        dslFactory.job(extraVars.get("FOLDER_NAME","Monitoring") + "/jenkins-heartbeat") {
            description("Job to check in with Dead Man's Snitch to make sure that Jenkins is still running.")

            logRotator {
                daysToKeep(1)
            }
            wrappers common_wrappers

            triggers {
                cron("H/5 * * * *")
            }
            steps {
                shell('curl ' + extraVars.get('SNITCH'))
            }

        }

    }
}

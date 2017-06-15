/*
    Job to check if Jenkins is successfully running
*/
package devops.jobs
import javaposse.jobdsl.dsl.DslFactory

class JenkinsHeartbeat{
    public static job( DslFactory dslFactory, Map extraVars){
        dslFactory.job(extraVars.get("FOLDER_NAME","Monitoring") + "/jenkins-heartbeat") {
            description("Job to check in with Dead Man's Snitch to make sure that Jenkins is still running.")
            triggers {
                cron("H/5 * * * *")
            }
            steps {
                shell('curl https://nosnch.in/35fcaf3a31')
            }

        }

    }
}
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
            description("Job to check in with Opsgenie heartbeat to make sure that Jenkins is still running.")

            logRotator {
                daysToKeep(1)
            }
            wrappers common_wrappers

            wrappers {
                credentialsBinding {
                    string("GENIE_KEY", "opsgenie_heartbeat_key")
                }
            }

            triggers {
                cron("H/5 * * * *")
            }
            steps {
                String opsgenie_heartbeat_name = extraVars.get('OPSGENIE_HEARTBEAT_NAME','')
                if (opsgenie_heartbeat_name) {
                    shell('curl -X GET "https://api.opsgenie.com/v2/heartbeats/'+opsgenie_heartbeat_name+'/ping" -H "Authorization: GenieKey ${GENIE_KEY}"')
                }
            }

        }

    }
}

/*
   This create a SandboxCIHourly and SandboxCIDaily job that run the CreateSandbox job,
   targetting an int and int-nightly sandbox for testing.  It's assumed you will seed this
   at the same time you seed CreateSandbox, since any interface changes will require updating
   these jobs.

   Variables consumed from the EXTRA_VARS input to your seed job in addition
   to those listed in the seed job.

   * FOLDER_NAME: "Sandboxes"
   * ACCESS_CONTROL: List of org or org*team from GitHub who get access to the jobs
   * HOURLY_SNITCH: URL of the snitch for the hourly CI job
   * DAILY_SNITCH: UTL of the snitch for the daily CI job
   * NOTIFY_ON_FAILURE: email address for failures

*/
package devops.jobs

import static org.edx.jenkins.dsl.DevopsConstants.common_wrappers
import static org.edx.jenkins.dsl.DevopsConstants.common_logrotator
import static org.edx.jenkins.dsl.DevopsConstants.common_read_permissions

class CreateSandboxCI {
  public static def jobs = { dslFactory, extraVars ->

    ["Hourly","Daily"].each { type ->
      dslFactory.job(extraVars.get("FOLDER_NAME","Sandboxes") + "/SandboxCI" + type) {
        wrappers common_wrappers
        logRotator common_logrotator
        throttleConcurrentBuilds {
            maxTotal(1)
        }

        def access_control = extraVars.get('ACCESS_CONTROL',[])
        access_control.each { acl ->
            common_read_permissions.each { perm ->
                authorization {
                    permission(perm,acl)
                }
            }
        }

        steps {
          downstreamParameterized {
            trigger(extraVars.get("FOLDER_NAME","Sandboxes") + "/CreateSandbox") {
              block {
                buildStepFailure('FAILURE')
                failure('FAILURE')
                unstable('UNSTABLE')
              }
              parameters {
                booleanParam('VERBOSE',true)
                // Once we resolve the mismatched Elastic Search versions, enable discovery
                //booleanParam('course_discovery',true)
                if (type == 'Daily') {
                  predefinedProp('server_type', 'full_edx_installation_from_scratch')
                  predefinedProp('dns_name','int-nightly')
                  predefinedProp('name_tag','continuous-integration-nightly')
                } else {
                  predefinedProp('dns_name','int')
                  booleanParam('reconfigure',true)
                  booleanParam('recreate',false)
                  predefinedProp('name_tag','continuous-integration')
                  booleanParam('ecommerce_worker',true)
                  booleanParam('credentials',true)
                }
              }
            }
          }
          shell('curl '+extraVars.get(type.toUpperCase()+"_SNITCH"))
        }

        triggers {
          cron(extraVars.get(type.toUpperCase()+"_SCHEDULE"))
        }

        publishers {
          mailer(extraVars.get('NOTIFY_ON_FAILURE',''), false, false)
        }
      }
    }
  }
}

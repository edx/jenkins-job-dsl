/*
   This creates a SandboxCIHourly, SandboxCIDaily, and SandboxCIMastersWeekly job
   that runs the CreateSandbox job, targeting an int, int-nightly, and masters-weekly sandbox for testing.
   It's assumed you will seed this at the same time you seed CreateSandbox,
   since any interface changes will require updating these jobs.

   Variables consumed from the EXTRA_VARS input to your seed job in addition
   to those listed in the seed job.

   * FOLDER_NAME: "Sandboxes"
   * ACCESS_CONTROL: List of org or org*team from GitHub who get access to the jobs
   * {DAILY,HOURLY,MASTERSWEEKLY,PROSPECTUSEVERYTWOHOURS}_SCHEDULE: When to run jobs, cron-formatted.
   * NOTIFY_ON_FAILURE: email address for failures
*/
package devops.jobs

import static org.edx.jenkins.dsl.DevopsConstants.common_wrappers
import static org.edx.jenkins.dsl.DevopsConstants.common_logrotator
import static org.edx.jenkins.dsl.DevopsConstants.common_read_permissions

class CreateSandboxCI {
  public static def jobs = { dslFactory, extraVars ->

    ["Hourly", "Daily", "MastersWeekly", "ProspectusEveryTwoHours"].each { type ->
      dslFactory.job(extraVars.get("FOLDER_NAME","Sandboxes") + "/SandboxCI" + type) {
        wrappers common_wrappers
        logRotator common_logrotator
        throttleConcurrentBuilds {
            maxTotal(1)
        }

        wrappers {
            credentialsBinding {
                string("GENIE_KEY", "opsgenie_heartbeat_key")
            }
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
                if (type == 'Hourly') {
                  predefinedProp('server_type', 'full_edx_installation_from_scratch')
                  predefinedProp('dns_name','int')
                  booleanParam('reconfigure',true)
                  booleanParam('recreate', true)
                  predefinedProp('name_tag','continuous-integration')
                  booleanParam('ecommerce_worker',true)
                  booleanParam('credentials',true)
                } else if (type == 'Daily') {
                  predefinedProp('server_type', 'full_edx_installation_from_scratch')
                  predefinedProp('dns_name','int-nightly')
                  predefinedProp('name_tag','continuous-integration-nightly')

                // Masters integration environment weekly CI.
                // Monitored by Registrar supporting team:
                // https://github.com/edx/registrar/blob/master/openedx.yaml
                } else if (type == 'MastersWeekly') {
                  predefinedProp('dns_name','masters-weekly')
                  predefinedProp('name_tag','masters-weekly')

                  // Define to 30 because:
                  // * This will be re-built every week anyway
                  // * We want real Masters integration boxes to be able to use
                  //   these parameters as defaults.
                  predefinedProp('sandbox_life','30')

                  // Explicitly define required services, even those with
                  // acceptable defaults, in case the defaults change.
                  // We want this build to be as consistent as possible.
                  booleanParam("edxapp",true)
                  booleanParam("ecommerce",true)
                  booleanParam("discovery",true)
                  booleanParam("registrar",true)
                  booleanParam("learner_portal",true)
                  booleanParam('testcourses',false)
                  booleanParam('performance_course',false)
                  booleanParam("demo_test_course",false)
                  booleanParam("edx_demo_course",false)
                  booleanParam("forum",false)
                  booleanParam("notifier",false)
                  booleanParam("xqueue",false)
                  booleanParam("ecommerce_worker",false)
                  booleanParam("certs",false)
                  booleanParam("analyticsapi",false)
                  booleanParam("insights",false)
                  booleanParam("demo",false)
                  booleanParam("credentials",false)
                  booleanParam("journals",false)
                  booleanParam("video_pipeline",false)
                  booleanParam("video_encode_worker",false)
                } else if (type == 'ProspectusEveryTwoHours') {
                  predefinedProp('dns_name','edx-rebrand')
                  predefinedProp('name_tag','edx-rebrand')

                  predefinedProp('server_type', 'full_edx_installation_from_scratch')
                  booleanParam('reconfigure', true)
                  booleanParam('edxapp', false)
                  booleanParam('testcourses', false)
                  booleanParam('performance_course', false)
                  booleanParam('demo_test_course', false)
                  booleanParam('edx_demo_course', false)
                  booleanParam('forum', false)
                  booleanParam('prospectus', true)
                  booleanParam('ecommerce', false)
                }
              }
            }
          }
          if (type == 'MastersWeekly') {
            downstreamParameterized {
              trigger(extraVars.get("FOLDER_NAME","Sandboxes") + "/UpdateMastersSandbox") {
                block {
                  buildStepFailure('FAILURE')
                  failure('FAILURE')
                  unstable('UNSTABLE')
                }
                parameters {
                  predefinedProp('dns_name','masters-weekly')
                }
              }
            }
          }
          shell('curl -X GET "https://api.opsgenie.com/v2/heartbeats/SandboxCI'+type+'/ping" -H "Authorization: GenieKey ${GENIE_KEY}"')
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

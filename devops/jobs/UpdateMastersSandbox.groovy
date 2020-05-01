/*
   This job takes a sandbox name, an Organization key, and program UUID/key mappings
   and creates a UpdateMastersSandbox job, which:
   * Syncs the sandbox's Discovery instance with production catalog data for the Organization
   * Refreshes its LMS catalog cache
   * Generates any missing course overviews the sandbox's LMS
   * Adds any missing programs to the sandbox's Registrar instance
   * Sets the external keys of the programs in Registrar based on the UUID/key mapping,
     defaulting to the marketing slug

   Variables consumed from the EXTRA_VARS input to your seed job in addition
   to those listed in the seed job.

   * FOLDER_NAME: "Sandboxes"
   * ACCESS_CONTROL: List of org or org*team from GitHub who get access to the jobs

   This job expects the sandbox-ssh-keys credential to contain an ssh key it can user
   to access any sandbox.

   This job also expects the strings sandbox-masters-automation-client-id and
   sandbox-masters-automation-client-secret to be set, giving the masters sandbox
   ansible task the ability to pull data from the production catalog API.

*/
package devops.jobs

import static org.edx.jenkins.dsl.DevopsConstants.common_wrappers
import static org.edx.jenkins.dsl.DevopsConstants.common_logrotator
import static org.edx.jenkins.dsl.DevopsConstants.common_read_permissions

class UpdateMastersSandbox {
  public static def job = { dslFactory, extraVars ->
    return dslFactory.job(extraVars.get("FOLDER_NAME","Sandboxes") + "/UpdateMastersSandbox") {

      description(
        "Sync a Sandbox's Discovery instance with production catalog data for an Organization, "
        + "refresh the LMS catalog cache, "
        + "generate any missing course overviews on LMS, "
        + "and add any missing programs to Registrar."
      )

      wrappers common_wrappers

      wrappers {
          credentialsBinding {
              string('MASTERS_AUTOMATION_CLIENT_ID', 'sandbox-masters-automation-client-id')
              string('MASTERS_AUTOMATION_CLIENT_SECRET', 'sandbox-masters-automation-client-secret')
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

      logRotator common_logrotator

      multiscm {
        git {
          remote {
            url('https://github.com/edx/configuration.git')
            branch('master')
          }
          extensions {
            cleanAfterCheckout()
            pruneBranches()
          }
        }
      }

      parameters {
        stringParam("dns_name","univ-of-change-me",
                    "DNS name of sandbox to update. The sandbox must have been built with Registrar enabled. "
                    + "Example: if your sandbox is unseen.sandbox.edx.org, enter 'unseen' here."
        )
        textParam("program_uuids",
                  "ef82b8e2-49ed-45b0-982d-c836deef507b:special-masters",
                  "List of UUIDs for programs that will be loaded from production Discovery. "
                  + "Separate program UUIDs with commas. "
                  + "You may set a program's external by appending ':external_key'"
                  + "to the UUID."
        )
      }

      properties {
        rebuild {
          autoRebuild(false)
          rebuildDisabled(false)
        }
      }

      concurrentBuild()

      wrappers {
        sshAgent('sandbox-ssh-keys')
      }

      steps {
        virtualenv {
          pythonName('System-CPython-3.6')
          nature("shell")
            systemSitePackages(false)
            command(dslFactory.readFileFromWorkspace("devops/resources/update-masters-sandbox.sh"))
        }
      }
    }
  }
}

/*
   This job takes a sandbox name and an Organization key and creates a UpdateMastersSandbox job, which:
   * Syncs the sandbox's Discovery instance with production catalog data for the Organization
   * Refresh its LMS catalog cache
   * Generate any missing course overviews the sandbox's LMS
   * and add any missing programs to the sandbox's Registrar instance

   Variables consumed from the EXTRA_VARS input to your seed job in addition
   to those listed in the seed job.

   * FOLDER_NAME: "Sandboxes"
   * ACCESS_CONTROL: List of org or org*team from GitHub who get access to the jobs
   * SSH_USER: ssh username that we can use to access the sandbox and sudo to add a user

   This job expects the sandbox-ssh-keys credential to contain an ssh key it can user
   to access any sandbox.
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
        stringParam("sandbox",'${BUILD_USER_ID}.sandbox.edx.org',"Optional - Only change this if you want to give access to a sandbox other than your default sandbox")
        stringParam("org_key","edX",'Key of the Organization whose catalog data will be synced with production.')
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

      environmentVariables {
        env('SSH_USER',extraVars.get('SSH_USER','ubuntu'))
      }

      steps {
        virtualenv {
          nature("shell")
            systemSitePackages(false)
            command(dslFactory.readFileFromWorkspace("devops/resources/update-masters-sandbox.sh"))
        }
      }
    }
  }
}

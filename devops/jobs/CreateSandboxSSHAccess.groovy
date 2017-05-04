/*

   This job creates a GrantSSHAccess job which adds an additional
   use to your sandbox.

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

class CreateSandboxSSHAccess {
  public static def job = { dslFactory, extraVars ->
    return dslFactory.job(extraVars.get("FOLDER_NAME","Sandboxes") + "/GrantSSHAccess") {

      description('Give ssh access to a github user.\nThen to log in:\n$ ssh YOUR_GITHUB_USERNAME@THE_SANDBOX_HOST_NAME')


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
        stringParam("user","","<b>Required</b> -  - github user to grant ssh access")
        stringParam("sandbox",'${BUILD_USER_ID}.sandbox.edx.org',"<b>Optional</b> - Only change this if you want to give access to a sandbox other than your default sandbox")
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
            command(dslFactory.readFileFromWorkspace("devops/resources/create-sandbox-ssh-access.sh"))
        }
      }
    }
  }
}

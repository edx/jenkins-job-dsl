/*

   This job creates a CreateSandboxCNAME job which allows you to set an alias
   for your existing sandbox.

   Variables consumed from the EXTRA_VARS input to your seed job in addition
   to those listed in the seed job.

   * FOLDER_NAME: "Sandboxes"
   * ACCESS_CONTROL: List of org or org*team from GitHub who get access to the jobs

   This job expects the following credentials to be defined on the folder

    sandbox-jenkins-aws-credentials: file with key/secret in boto config format
    sandbox-role-arn: the role to aws sts assume-role
*/
package devops.jobs

import static org.edx.jenkins.dsl.DevopsConstants.common_wrappers
import static org.edx.jenkins.dsl.DevopsConstants.common_logrotator
import static org.edx.jenkins.dsl.DevopsConstants.common_read_permissions

class CreateSandboxCNAME {
  public static def job = { dslFactory, extraVars ->
    return dslFactory.job(extraVars.get("FOLDER_NAME","Sandboxes") + "/CreateSandboxCNAME") {

      description("Sets a DNS Alias for your sandbox")

      wrappers common_wrappers

      def access_control = extraVars.get('ACCESS_CONTROL',[])
      access_control.each { acl ->
        common_read_permissions.each { perm ->
          authorization {
            permission(perm,acl)
          }
        }
      }

      wrappers {
        credentialsBinding {
          file('AWS_CONFIG_FILE','sandbox-jenkins-aws-credentials')
          string('ROLE_ARN','sandbox-role-arn')
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
        stringParam("dns_name","","<b>Required</b> - cname to create in the dns_zone. <br />\n<i>Example: test will create test.sandbox.edx.org")
        stringParam("sandbox",'${BUILD_USER_ID}.sandbox.edx.org',"<b>Optional</b> - Only change this if you want to alias a sandbox other than your default sandbox")
      }

      // We don't allow any other access from this box, so making this configurable is unnecesary
      environmentVariables {
        env('dns_zone','sandbox.edx.org')
      }

      properties {
        rebuild {
          autoRebuild(false)
            rebuildDisabled(false)
        }
      }

      concurrentBuild()

      steps {
        virtualenv {
          pythonName('System-CPython-3.6')
          nature("shell")
            systemSitePackages(false)
            command(dslFactory.readFileFromWorkspace("devops/resources/create-sandbox-cname.sh"))
        }
      }
    }
  }
}

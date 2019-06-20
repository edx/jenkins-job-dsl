/*

    Variables consumed from the EXTRA_VARS input to your seed job in addition
    to those listed in the seed job.

    * FOLDER_NAME: "Sandboxes"
    * BASIC_AUTH_USER 
    * BASIC_AUTH_PASS
    * ACCESS_CONTROL: List of orgs / orgs*teams who get github access
    * CONFIGURATION_SECURE_REPO (required)
    * CONFIGURATION_INTERNAL_REPO (required)
    * SSH_KEYPAIR_NAME (required)

    Credentials should be set up inside your FOLDER_NAME. Be sure your Jenkins Credential
    uses the id specified in this list or the created job will be unable to find the Credential.

    sandbox-jenkins-aws-credentials: file with key/secret in boto config format
    sandbox-role-arn: the role to aws sts assume-role
    sandbox-ssh-keys: ssh keypair used to log in to the sandbox and run ansible, usually equivalent to SSH_KEYPAIR_NAME
    sandbox-secure-credentials: an ssh key usable to fetch secure sandbox configuration (often a github deploy key)


*/
package devops.jobs

import static org.edx.jenkins.dsl.DevopsConstants.common_wrappers
import static org.edx.jenkins.dsl.DevopsConstants.common_read_permissions

import static devops.jobs.CreateSandbox.sandbox_multiscm

class CreateMastersSandbox {
    public static def job = { dslFactory, extraVars ->
        return dslFactory.job(extraVars.get("FOLDER_NAME","Sandboxes") + "/CreateMastersSandbox") {

            wrappers common_wrappers

            wrappers {
                buildName('#${BUILD_NUMBER} ${ENV,var="BUILD_USER_ID"} ${ENV,var="dns_name"}')
            }

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
                sshAgent('sandbox-ssh-keys')
            }

            logRotator {
                daysToKeep(30)
            }

            multiscm sandbox_multiscm

            environmentVariables {
                env("configuration_source_repo", "https://github.com/edx/configuration.git")

                env("edxapp",true)
                env("edx_platform_repo","https://github.com/edx/edx-platform.git")
                env("discovery",true)
                env("registrar",true)

                env("notifier",false)
                env("xqueue",false)
                env("xserver",false)
                env("ecommerce_worker",false)
                env("certs",false)
                env("analyticsapi",false)
                env("insights",false)
                env("demo",false,"")
                env("credentials",false)
                env("set_whitelabel",false)
                env("journals",false)
                env("video_pipeline",false)
                env("video_encode_worker",false)

                // This option is redundant with server_type,
                // so we set it to false.
                // Setting server_type == full_edx_installation_from_scratch
                // has the same effect as setting this to true.
                env("reconfigure",false)
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
                    nature("shell")
                    systemSitePackages(false)

                    command(dslFactory.readFileFromWorkspace("devops/resources/create-sandbox.sh"))

                }

            }

        }
    }
}

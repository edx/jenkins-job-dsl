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

class CreateSandbox {
    public static def job = { dslFactory, extraVars ->
        def jobName = extraVars.get("SANDBOX_JOB_NAME", "CreateSandbox")
        return dslFactory.job(extraVars.get("FOLDER_NAME","Sandboxes") + "/${jobName}") {

            wrappers common_wrappers

            wrappers {
                buildName('#${BUILD_NUMBER} ${ENV,var="BUILD_USER_ID"} ${ENV,var="dns_name"}')

                timeout {
                    absolute(120)
                    failBuild()
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

            multiscm {
                git {
                    remote {
                        url('$configuration_source_repo')
                        branch('$configuration_version')
                    }
                    extensions {
                        cleanAfterCheckout()
                        pruneBranches()
                        relativeTargetDirectory('configuration')
                    }
                }
                git {
                    remote {
                        url(extraVars.get('CONFIGURATION_SECURE_REPO',''))
                        branch('$configuration_secure_version')
                        credentials('sandbox-secure-credentials')
                    }
                    extensions {
                        cleanAfterCheckout()
                        pruneBranches()
                        relativeTargetDirectory('configuration-secure')
                    }
                }
                git {
                    remote {
                        url(extraVars.get('CONFIGURATION_INTERNAL_REPO',''))
                        branch('$configuration_internal_version')
                        credentials('sandbox-secure-credentials')
                    }
                    extensions {
                        cleanAfterCheckout()
                        pruneBranches()
                        relativeTargetDirectory('configuration-internal')
                    }
                }
            }



            parameters {
                booleanParam("recreate",true,"Checking this option will terminate an existing instance if it already exists and start over from scratch")
                stringParam("dns_name","",
                        "DNS name, if left blank will default to your GitHub username. \
                         One reason you might want to override this field is if you are building a sandbox for review or a specific task. \
                         If setting this, you probably want to also set name_tag below. \
                         For example, if you are building a sandbox for pull request 1234 put in 'pr1234' which will setup the sandbox <i>pr1234.sandbox.edx.org</i>.<br /> \
                         <b>Do not use hyphens or underscores here.</b> <br /> \
                         If you are building a sandbox for yourself, you may leave this blank <b>unless</b> your GitHub \
                         username contains underscores (which are invalid for domain names in URLs) \
                         or hyphens (which have broken some IDAs in sandboxes in the past). <br />")
                stringParam("name_tag","",
                        "This name tag uniquely identifies your sandbox.  <b>If a box already exists with this name tag, it will be terminated.</b><br /> \
                         If you want to have multiple sandboxes running simultaneously, you must give each one a unique name tag.")
                stringParam("sandbox_platform_name","","sets EDXAPP_PLATFORM_NAME, by default it will take your github username/sandbox dns name as value")
                stringParam("sandbox_life","7","Number of day(s) sandbox will be online(between 1 to 30)")
                booleanParam("VERBOSE",false,"Whether to run ansible in verbose mode.  Useful if debugging a sandbox failure")
                stringParam("configuration_version","master","")
                stringParam("configuration_source_repo","https://github.com/edx/configuration.git",
                            "If building a sandbox to test an external configuration PR, replace this with the fork of configuration.git's https URL")
                stringParam("configuration_secure_version","master",
                            "Select an alternative branch of sandbox-secure configuration repo")
                stringParam("configuration_internal_version","master",
                            "Select an alternative branch of sandbox-internal configuration repo")
                booleanParam("reconfigure",false,"Reconfigure and deploy, this will also run with --skip-tags deploy against all role <br />Leave this unchecked unless you know what you are doing")
                booleanParam("testcourses",true,"")
                booleanParam("performance_course",true,"")
                booleanParam("demo_test_course",true,"")
                booleanParam("edx_demo_course",true,"")

                booleanParam("edxapp",true,"")
                stringParam("edxapp_version","master","")
                stringParam("edx_platform_repo","https://github.com/edx/edx-platform.git",
                            "If building a sandbox to test an external configuration PR, replace this with the fork of configuration.git's https URL")
                booleanParam("edxapp_decrypt_and_copy_config_enabled",true,"Checking this option will decrypt and copy edxapp config file from configuration internal repo.")

                booleanParam("forum",true,"")
                stringParam("forum_version","master","")

                booleanParam("ecommerce",true,"")
                stringParam("ecommerce_version","master","")
                booleanParam("ecommerce_decrypt_and_copy_config_enabled",true,"Checking this option will decrypt and copy ecommerce config file from configuration internal repo.")

                booleanParam("xqueue",false,"")
                stringParam("xqueue_version","master","")

                booleanParam("xserver",false,"")
                stringParam("xserver_version","master","")

                booleanParam("ecommerce_worker",true,"")
                stringParam("ecommerce_worker_version","master","")
                booleanParam("ecommerce_worker_decrypt_and_copy_config_enabled",true,"Checking this option will decrypt and copy ecommerce_worker config file from configuration internal repo.")

                booleanParam("certs",false,"")
                stringParam("certs_version","master","")

                booleanParam("analyticsapi",false,"")
                stringParam("analytics_api_version","master","")
                booleanParam("analytics_api_decrypt_and_copy_config_enabled",true,"Checking this option will decrypt and copy analytics_api config file from configuration internal repo.")

                booleanParam("insights",false,"")
                stringParam("insights_version","master","")
                booleanParam("insights_decrypt_and_copy_config_enabled",true,"Checking this option will decrypt and copy insights config file from configuration internal repo.")

                booleanParam("demo",false,"")
                stringParam("demo_version","master","")

                booleanParam("discovery",false,"")
                stringParam("discovery_version","master","")
                booleanParam("discovery_decrypt_and_copy_config_enabled",true,"Checking this option will decrypt and copy discovery config file from configuration internal repo.")

                booleanParam("notes",false,"")
                stringParam("notes_version","master","")

                booleanParam("credentials",false,"")
                stringParam("credentials_version","master","")
                booleanParam("credentials_decrypt_and_copy_config_enabled",true,"Checking this option will decrypt and copy credentials config file from configuration internal repo.")

                stringParam("themes_version","master","")
                booleanParam("registrar",false,"Enable the Registrar service, along with the Program Manager micro-frontend")
                stringParam("registrar_version","master","")
                booleanParam("registrar_decrypt_and_copy_config_enabled",true,"Checking this option will decrypt and copy registrar config file from configuration internal repo.")
                stringParam("registrar_user_email","registrar@example.com",
                            "Email (and username) for user to be created in Registrar. Ignore this setting if Registrar is disabled.")
                stringParam("registrar_org_key","edX",
                            "Key for Organization to be created in Registrar. Must match key in Discovery catalog. Ignore this setting if Registrar is disabled.")
                stringParam("program_console_version","master",
                            "The repository version of the frontend-app-program-console")

                booleanParam("learner_portal",false,"Learner Portal")
                stringParam("learner_portal_version","master","The version for the frontend-app-learner-portal")

                 booleanParam("admin_portal",false,"Enable Enterprise Admin Portal MFE")
                stringParam("admin_portal_version","master","The repository version of the frontend-app-admin-portal")

                booleanParam("video_pipeline",false,
                             "video_pipeline and video_encode_worker must be selected for video pipeline to work")
                stringParam("video_pipeline_version","master","")

                booleanParam("video_encode_worker",false,
                             "video_pipeline and video_encode_worker must be selected for video pipeline to work")
                stringParam("video_encode_worker_version","master","")

                booleanParam("prospectus",false,"Enable Prospectus, the edx catalog")
                stringParam("prospectus_version","master","")

                booleanParam("authn",false,"Enable Authn MFE")
                stringParam("authn_version","master","The repository version of the frontend-app-authn")

                booleanParam("payment",false,"Enable Payment MFE")
                stringParam("payment_version","master","The repository version of the frontend-app-payment")

                choiceParam("server_type",
                            [
                            "full_edx_installation_from_scratch",
                            "full_edx_installation",
                             "ubuntu_18.04",
                             "ubuntu_20.04",
                             "ubuntu_16.04",
                            ],
                            "Type of AMI we should boot before updating the selected roles above")

                stringParam("github_username","","Github account whose ssh keys will be used to set up an account on the sandbox.  Defaults to your jenkins account, which comes from github")

                stringParam("region","us-east-1","")

                stringParam("aws_account","sandbox","")

                stringParam("keypair",extraVars.get('SSH_KEYPAIR_NAME'),"")

                choiceParam("root_ebs_size",
                            ["50",
                             "100",
                             "150",
                             "200",
                             "250",
                             "300",
                             "350",
                             "400",
                             "450",
                             "500"],
                            "Root volume size (in GB)")

                stringParam("security_group","sandbox-vpc","")

                stringParam("dns_zone","sandbox.edx.org","Please don't modify unless you know what you're doing.")

                stringParam("environment","sandbox","")

                stringParam("instance_type","r5.large","We have reservations for the default size to keep costs down, please don't change this to something larger without talking to devops")

                stringParam("ami","","Leave blank to use the default ami for your server type.")

                stringParam("vpc_subnet_id","","")

                booleanParam("basic_auth",true,"")

                stringParam("auth_user",extraVars.get('BASIC_AUTH_USER',''),"")

                stringParam("auth_pass",extraVars.get('BASIC_AUTH_PASS',''),"")

                booleanParam("enable_automatic_auth_for_testing",false,"This enables the /auto_auth endpoint which facilitates generating fake users.  This is typically required for running load tests.")

                booleanParam("start_services",true,"")

                booleanParam("edx_internal",true,
                             "Keep this checked for sandbox use.  Only uncheck if you want an image that will be distributed outside of edX and should not have any edX private data on it (such as SSL certificates, xserver information, etc.).")

                booleanParam("enable_newrelic",false,"Enable NewRelic application monitoring (this costs money, please ask devops before enabling). Server level New Relic monitoring is always enabled.  Select 'reconfigure' as well, if you want to deploy this.")

                booleanParam("enable_client_profiling",false,"Enable the SESSION_SAVE_EVERY_REQUEST django setting for client profiling.")

                booleanParam("run_oauth",true,"")

                stringParam("nginx_users",'[{"name": "{{ COMMON_HTPASSWD_USER }}","password": "{{ COMMON_HTPASSWD_PASS }}","state":"present"}]',"")
            }


            properties {
                rebuild {
                    autoRebuild(false)
                    rebuildDisabled(false)
                }
            }

            concurrentBuild()

            steps {
                shell(dslFactory.readFileFromWorkspace('devops/resources/create-sandbox.sh'))
            }

        }
    }
}

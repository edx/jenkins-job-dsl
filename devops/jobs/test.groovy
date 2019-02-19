
package devops.jobs

import static org.edx.jenkins.dsl.DevopsConstants.common_wrappers
import static org.edx.jenkins.dsl.DevopsConstants.common_read_permissions

class test {
    public static def job = { dslFactory ->
        def jobName =  "test"
        return dslFactory.job("${jobName}") {

            logRotator {
                daysToKeep(5)
            }

            multiscm {
                git {
                    remote {
                        url('https://github.com/mckinseyacademy/mcka_apros.git')
                        branch('development')
                    }
                    extensions {
                        cleanAfterCheckout()
                        pruneBranches()
                        relativeTargetDirectory('mcka_apros')
                    }
                }
            }

            parameters {
                booleanParam("recreate",true,"Checking this option will terminate an existing instance if it already exists and start over from scratch")
                stringParam("dns_name","",
                        "DNS name, if left blank will default to your github username. \
                         One reason you might want to override this field is if you are building a sandbox for review or a specific task. \
                         If setting this, you probably want to also set name_tag below. \
                         For example, if you are building a sandbox for pull request 1234 put in 'pr1234' which will setup the sandbox <i>pr1234.sandbox.edx.org</i>.<br /> \
                         <b>If you are building a sandbox for yourself leave this blank</b><b>Do not use underscores</b>")
                stringParam("name_tag","",
                        "This name tag uniquely identifies your sandbox.  <b>If a box already exists with this name tag, it will be terminated.</b><br /> \
                         If you want to have multiple sandboxes running simultaneously, you must give each one a unique name tag.")
                stringParam("sandbox_platform_name","","sets EDXAPP_PLATFORM_NAME, by default it will take your github username/sandbox dns name as value")
                stringParam("sandbox_life","7","Number of day(s) sandbox will be online(between 1 to 30)")
                booleanParam("VERBOSE",false,"Whether to run ansible in verbose mode.  Useful if debugging a sandbox failure")
                stringParam("configuration_version","master","")
                stringParam("configuration_source_repo","https://github.com/edx/configuration.git",
                            "If building a sandbox to test an external configuration PR, replace this with the fork of configuration.git's https URL")
                stringParam("configuration_secure_version","master","")
                stringParam("configuration_internal_version","master","")
                booleanParam("reconfigure",false,"Reconfigure and deploy, this will also run with --skip-tags deploy against all role <br />Leave this unchecked unless you know what you are doing")
                booleanParam("testcourses",true,"")
                booleanParam("performance_course",true,"")
                booleanParam("demo_test_course",true,"")
                booleanParam("edx_demo_course",true,"")

                booleanParam("edxapp",true,"")
                stringParam("edxapp_version","master","")
                stringParam("edx_platform_repo","https://github.com/edx/edx-platform.git",
                            "If building a sandbox to test an external configuration PR, replace this with the fork of configuration.git's https URL")

                booleanParam("forum",true,"")
                stringParam("forum_version","master","")

                booleanParam("ecommerce",true,"")
                stringParam("ecommerce_version","master","")

                booleanParam("notifier",false,"")
                stringParam("notifier_version","master","")

                booleanParam("xqueue",false,"")
                stringParam("xqueue_version","master","")

                booleanParam("xserver",false,"")
                stringParam("xserver_version","master","")

                booleanParam("ecommerce_worker",true,"")
                stringParam("ecommerce_worker_version","master","")

                booleanParam("certs",false,"")
                stringParam("certs_version","master","")

                booleanParam("analyticsapi",false,"")
                stringParam("analytics_api_version","master","")

                booleanParam("insights",false,"")
                stringParam("insights_version","master","")

                booleanParam("demo",false,"")
                stringParam("demo_version","master","")

                booleanParam("discovery",false,"")
                stringParam("discovery_version","master","")

                booleanParam("credentials",false,"")
                stringParam("credentials_version","master","")

                booleanParam("set_whitelabel",false,
                             "Check this in order to create a Sandbox with whitelabel themes automatically set.")
                stringParam("themes_version","master","")

                booleanParam("journals",false,"Journals service")
                stringParam("journals_version","master","")

                booleanParam("video_pipeline",false,
                             "video_pipeline and video_encode_worker must be selected for video pipeline to work")
                stringParam("video_pipeline_version","master","")

                booleanParam("video_encode_worker",false,
                             "video_pipeline and video_encode_worker must be selected for video pipeline to work")
                stringParam("video_encode_worker_version","master","")

                choiceParam("server_type",
                            ["full_edx_installation",
                             "full_edx_installation_from_scratch",
                             "ubuntu_16.04",
                            ],
                            "Type of AMI we should boot before updating the selected roles above")

                stringParam("github_username","","Github account whose ssh keys will be used to set up an account on the sandbox.  Defaults to your jenkins account, which comes from github")

                stringParam("region","us-east-1","")

                stringParam("aws_account","sandbox","")


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

                virtualenv {
                    nature("shell")
                    systemSitePackages(false)

                    command(dslFactory.readFileFromWorkspace("devops/resources/create-sandbox.sh"))

                }

            }

        }
    }
}

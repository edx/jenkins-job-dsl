
            parameters {

                // Naming
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

                // Basic auth
                booleanParam("basic_auth",true,"")
                stringParam("auth_user","changeme","")
                stringParam("auth_pass","changeme","")

                // Service versions
                stringParam("edxapp_version","master","edxapp is always enabled, but you may specify a branch/commit/tag.")
                booleanParam("enable_automatic_auth_for_testing",false,"This enables the /auto_auth endpoint which facilitates generating fake users.  This is typically required for running load tests.")
                stringParam("discovery_version","master","discovery is always enabled, but you specify a branch/commit/tag.")
                stringParam("registrar_version","master","registrar is always enabled, but you specify a branch/commit/tag.")

                // Config versions
                stringParam("configuration_version","master","branch/commit/tag to use for edx/configuration.git")
                stringParam("configuration_internal_version","master","branch/commit/tag to use for edx/edx-internal.git")
                stringParam("configuration_secure_version","master","branch/commit/tag to use for secure configuration repository")
                booleanParam("edx_internal",true,
                             "Keep this checked for sandbox use.  Only uncheck if you want an image that will be distributed outside of edX and should not have any edX private data on it (such as SSL certificates, xserver information, etc.).")

                // From scratch?
                choiceParam("server_type",
                            ["full_edx_installation_from_scratch",
                             "full_edx_installation",
                             "ubuntu_16.04",
                            ],
                            "Type of AMI we should boot before updating the selected roles above")

                // Test data
                booleanParam("testcourses",true,"")
                booleanParam("performance_course",true,"")
                booleanParam("demo_test_course",true,"")
                booleanParam("edx_demo_course",true,"")

                // Ecommerce?
                booleanParam("ecommerce",true,"")
                stringParam("ecommerce_version","master","")

                // Forum?
                booleanParam("forum",false,"")
                stringParam("forum_version","master","")

                // Misc. options
                booleanParam("VERBOSE",false,"Whether to run ansible in verbose mode.  Useful if debugging a sandbox failure")
                booleanParam("recreate",true,"Checking this option will terminate an existing instance if it already exists and start over from scratch")
                stringParam("github_username","","Github account whose ssh keys will be used to set up an account on the sandbox.  Defaults to your jenkins account, which comes from github")
                stringParam("keypair",extraVars.get('SSH_KEYPAIR_NAME'),"")

                // Stuff we probably don't want to change
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
                booleanParam("enable_newrelic",false,"Enable NewRelic application monitoring (this costs money, please ask devops before enabling). Server level New Relic monitoring is always enabled.  Select 'reconfigure' as well, if you want to deploy this.")
                booleanParam("enable_client_profiling",false,"Enable the SESSION_SAVE_EVERY_REQUEST django setting for client profiling.")
                booleanParam("start_services",true,"")
                booleanParam("run_oauth",true,"")
                stringParam("nginx_users",'[{"name": "{{ COMMON_HTPASSWD_USER }}","password": "{{ COMMON_HTPASSWD_PASS }}","state":"present"}]',"")
            }

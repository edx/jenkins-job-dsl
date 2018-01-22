/*

    Variables consumed from the EXTRA_VARS input to your seed job in addition
    to those listed in the seed job.

    EXTRA_VARS: Additional vars to be passed to ansible-playbook with -e (e.g. -e foo=bar)
    EXTRA_VARS_FILES: Additional vars files to be passed to ansible playbook with -e@ (e.g. -e@/path/to/env-dep.yml)
    SSH_AGENT_KEY: dictionary of key per environment (looked up from the currently generating ENVIRONMENT)
    CONFIGURATION_REPO: standard default available
    CONFIGURATION_BRANCH: defaults to master
    ACCESS_CONTROL: list of Jenkins matrix targets who should get read access (edx*team or username)
    POLL_SCM: Cron syntax for polling SCM for changes
    SCHEDULE: Cron syntax consumed by Jenkins triggered builds
    REGION: For boto - defaults to us-east-1
    SSH_USER: ansible connect user - defaults to ubuntu.  Make sure this matches up with SSH_AGENT_KEY
    CLUSTER: Our cluster tag for identifying machines
    PATTERN: host pattern consumed by ansible (normally an argument to ec2.py)
    INVENTORY: ansible inventory - defaults to ec2.py
    CUSTOM_INVENTORY: if you want to run a custom program to generate an inventory, set this and it will override INVENTORY
    NOTIFY_ON_FAILURE: email address to be notified when this job fails

    Expected credentials - these will normally be set up on the Folder.
        jnkins-aws-credentials: a file credential that can be set in the environment as AWS_CONFIG_FILE for assuming role
        find-host-role-arn: ARN of the IAM role that will be assumed

*/
package devops.jobs

import static org.edx.jenkins.dsl.DevopsConstants.common_wrappers
import static org.edx.jenkins.dsl.DevopsConstants.common_logrotator
import static org.edx.jenkins.dsl.DevopsConstants.common_read_permissions

class RunAnsible {
    public static def job = { dslFactory, jobName, environment, deployment, playbook, extraVars ->
        return dslFactory.job(extraVars.get("FOLDER_NAME") + "/${environment}-${deployment}-${jobName}") {
            /*
               Run arbitrary remote commands on a host belonging to a target environment, deployment and cluster,
               in a specified region.
             */

            wrappers common_wrappers
            wrappers {
                credentialsBinding {
                    file('AWS_CONFIG_FILE','jenkins-aws-credentials')
                    string('ROLE_ARN','find-host-role-arn')
                }
                sshAgent(extraVars.get("SSH_AGENT_KEY").get(environment))
            }

            parameters {
                stringParam('CONFIGURATION_REPO', extraVars.get('CONFIGURATION_REPO', 'https://github.com/edx/configuration.git'),
                            'Git repo containing the analytics pipeline configuration automation.')
                stringParam('CONFIGURATION_BRANCH', extraVars.get('CONFIGURATION_BRANCH', 'master'),
                            'e.g. tagname or origin/branchname')
            }

            def access_control = extraVars.get('ACCESS_CONTROL',[])
            access_control.each { acl ->
                common_read_permissions.each { perm ->
                    authorization {
                        permission(perm,acl)
                    }
                }
            }

            multiscm {
                git {
                    remote {
                        url('$CONFIGURATION_REPO')
                        branch('$CONFIGURATION_BRANCH')
                    }
                    extensions {
                        cleanAfterCheckout()
                        pruneBranches()
                        relativeTargetDirectory('configuration')
                    }
                }
            }

            triggers {
                if (extraVars.get('SCHEDULE')) {
                    cron(extraVars['SCHEDULE'])
                }

                if (extraVars.get('POLL_SCM')) {
                    scm(extraVars['POLL_SCM'])
                }
            }
            throttleConcurrentBuilds {
                maxPerNode(1)
            }

            // Parameters from the seed job that you don't override, they're just part of
            // the run.  We need to load different ssh keys per env anyway.
            environmentVariables {
                env('REGION', extraVars.get('REGION','us-east-1'))
                env('ENVIRONMENT', environment)
                env('DEPLOYMENT', deployment)
                env('ANSIBLE_SSH_USER', extraVars.get('SSH_USER','ubuntu'))
                env('CLUSTER', extraVars.get('CLUSTER'))
                env('BECOME_USER', extraVars.get('BECOME_USER',''))
                env('ANSIBLE_MODULE_NAME', extraVars.get('MODULE_NAME','shell'))
                env('ANSIBLE_MODULE_ARGS', extraVars.get('MODULE_ARGS'))
                env('PATTERN', extraVars.get('PATTERN',''))
                env('INVENTORY', extraVars.get('INVENTORY',''))
                env('CUSTOM_INVENTORY', extraVars.get('CUSTOM_INVENTORY',''))
            }

            steps {
                virtualenv {
                    name(jobName)
                    nature('shell')
                    command(dslFactory.readFileFromWorkspace('devops/resources/run-ansible.sh'))
                }
            }

            publishers {
                if (extraVars.get('NOTIFY_ON_FAILURE')) {
                    mailer(extraVars.get('NOTIFY_ON_FAILURE'), false, false)
                }
            }
        }
    }
}

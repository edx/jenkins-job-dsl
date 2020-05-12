/*

    Variables consumed from the EXTRA_VARS input to your seed job in addition
    to those listed in the seed job.

    SSH_AGENT_KEY: dictionary of key per environment-deployment (looked up from the currently generating ENVIRONMENT and DEPLOYMENT)
        As a backwards compatibility shim, will also fall back to environment only until all deployed jobs are updated.
    CONFIGURATION_REPO: standard default available
    CONFIGURATION_BRANCH: defaults to master
    ACCESS_CONTROL: list of Jenkins matrix targets who should get read access (edx*team or username)
    SCHEDULE: Cron syntax consumed by Jenkins triggered builds
    REGION: For boto - defaults to us-east-1
    SSH_USER: ansible connect user - defaults to ubuntu.  Make sure this matches up with SSH_AGENT_KEY
    CLUSTER: Our cluster tag for identifying machines
    BECOME_USER: If you are sudoing using ansible, this sets ansible's become
    MODULE_NAME: What ansible module to run, defaults to shell
    MODULE_ARGS: Arguments for the ansible module, must be specified
    MODULE_ARGS_SHIM: Arguments for the ansible module using docker shim, optional
    PATTERN: host pattern consumed by ansible (normally an argument to ec2.py)
    INVENTORY: ansible inventory - defaults to ec2.py
    CUSTOM_INVENTORY: if you want to run a custom program to generate an inventory, set this and it will override INVENTORY
    NOTIFY_ON_FAILURE: email address to be notified when this job fails

    Expected credentials - these will normally be set up on the Folder.
        jnkins-aws-credentials: a file credential that can be set in the environment as AWS_CONFIG_FILE for assuming role
            This name can be overridden by passing a credential id as AWS_CONFIG_FILE in the extra vars
        find-host-role-arn: ARN of the IAM role that will be assumed
            This name can be overridden by passing an ARN as ROLE_ARN in the extra vars

*/
package devops.jobs

import javaposse.jobdsl.dsl.DslFactory

import static org.edx.jenkins.dsl.DevopsConstants.common_wrappers
import static org.edx.jenkins.dsl.DevopsConstants.common_logrotator
import static org.edx.jenkins.dsl.DevopsConstants.common_read_permissions

class RunAnsible {
    public static job(DslFactory dslFactory, String jobName, String environment, String deployment, Map extraVars) {
        return dslFactory.job(extraVars.get("FOLDER_NAME") + "/${environment}-${deployment}-${jobName}") {
            // These 3 don't have defaults
            assert extraVars.containsKey('SSH_AGENT_KEY') : "Please define SSH_AGENT_KEY"
            assert extraVars.containsKey('CLUSTER') : "Please define CLUSTER"
            assert extraVars.containsKey('MODULE_ARGS') : "Please define MODULE_ARGS"
            /*
               Run arbitrary remote commands on a host belonging to a target environment, deployment and cluster,
               in a specified region.
             */
            wrappers common_wrappers
            logRotator common_logrotator
            wrappers {
                credentialsBinding {
                    file('AWS_CONFIG_FILE', extraVars.get('AWS_CONFIG_FILE','jenkins-aws-credentials'))
                    string('ROLE_ARN',extraVars.get('AWS_ROLE_ARN','find-host-role-arn'))
                }
                def ssh_key = extraVars["SSH_AGENT_KEY"]["${environment}-${deployment}"]
                if (!ssh_key) {
                  // Backcompat shim until we reseed all the jobs and rename all the credentials
                  ssh_key = extraVars["SSH_AGENT_KEY"][environment]
                }
                assert ssh_key : "Unable to find an ssh key in SSH_AGENT_KEY.${environment}-${deployment} or SSH_AGENT_KEY.${environment}"
                sshAgent(ssh_key)
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
            }
            throttleConcurrentBuilds {
                maxPerNode(1)
            }

            // Parameters from the seed job that you don't override, they're just part of
            // the run.  We need to load different ssh keys per env anyway.
            environmentVariables {
                env("REGION", extraVars.get('REGION','us-east-1'))
                env("ENVIRONMENT", environment)
                env("DEPLOYMENT", deployment)
                env("ANSIBLE_SSH_USER", extraVars.get('SSH_USER','ubuntu'))
                env("CLUSTER", extraVars.get('CLUSTER'))
                env("BECOME_USER", extraVars.get('BECOME_USER',''))
                env('ANSIBLE_MODULE_NAME', extraVars.get('MODULE_NAME','shell'))
                env('ANSIBLE_MODULE_ARGS', extraVars.get('MODULE_ARGS'))
                env('ANSIBLE_MODULE_ARGS_SHIM', extraVars.get('MODULE_ARGS_SHIM',''))
                env('PATTERN', extraVars.get('PATTERN',''))
                env('INVENTORY', extraVars.get('INVENTORY',''))
                env('CUSTOM_INVENTORY', extraVars.get('CUSTOM_INVENTORY',''))
            }

            steps {
                virtualenv {
                    pythonName('System-CPython-3.6')
                    name(jobName)
                    nature('shell')
                    command(dslFactory.readFileFromWorkspace('devops/resources/run-ansible.sh'))
                }
            }

            publishers {
                mailer(extraVars.get('NOTIFY_ON_FAILURE',''), false, false)
            }
        }
    }
}

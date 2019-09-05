/*

    Variables consumed from the EXTRA_VARS input to your seed job in addition
    to those listed in the seed job.

    SSH_AGENT_KEY: Key for the SSH Agent
    CONFIGURATION_REPO: standard default available
    CONFIGURATION_BRANCH: defaults to master
    SSH_USER: ansible connect user - defaults to ubuntu.  Make sure this matches up with SSH_AGENT_KEY
    ANSIBLE_PLAYBOOK:  The playbook used in this job

    Expected credentials - these will normally be set up on the Folder.
        jnkins-aws-credentials: a file credential that can be set in the environment as AWS_CONFIG_FILE for assuming role
        find-host-role-arn: ARN of the IAM role that will be assumed
    Optional:
    ACCESS_CONTROL: list of Jenkins matrix targets who should get read access (edx*team or username)
    SCHEDULE: Cron syntax consumed by Jenkins triggered builds
    NOTIFY_ON_FAILURE: email address to be notified when this job fails
    ADDITIONAL_REPO: an additional git repository to be checkout out if needed
    ADDITIONAL_REPO_BRANCH: defaults to master
    ADDITIONAL_REPO_TARGET_DIRECTORY: name of additional repo target directory in the workspace
    ANSIBLE_EXTRA_VARS: Extra variables for Ansible
    TAGS: Tags for ansible
*/
package devops.jobs

import static org.edx.jenkins.dsl.DevopsConstants.common_wrappers
import static org.edx.jenkins.dsl.DevopsConstants.common_logrotator
import static org.edx.jenkins.dsl.DevopsConstants.common_read_permissions

class RunRemoteAnsiblePlaybook {
    public static def job = { dslFactory, jobName, extraVars ->
        return dslFactory.job("${jobName}") {

            // Common Jenkins Job features
            wrappers common_wrappers

            wrappers {
              sshAgent(extraVars.get("SSH_AGENT_KEY", ''))
            }

            parameters {
                stringParam('CONFIGURATION_REPO', extraVars.get('CONFIGURATION_REPO', 'https://github.com/edx/configuration.git'),
                            'Git repo containing the analytics pipeline configuration automation.')
                stringParam('CONFIGURATION_BRANCH', extraVars.get('CONFIGURATION_BRANCH', 'master'),
                            'e.g. tagname or origin/branchname')
                stringParam("ANSIBLE_INVENTORY", extraVars.get('ANSIBLE_INVENTORY', ""),
                            "The inventory address(es) to run the remote script")
                stringParam("ANSIBLE_EXTRA_VARS", extraVars.get('ANSIBLE_EXTRA_VARS', ""),
                            "The Extra variables for Ansible for example '-e ora2_version=''")
                stringParam("NOTIFY_ON_FAILURE", extraVars.get('NOTIFY_ON_FAILURE', ""),
                            "Email to notify on failure")
                stringParam("ANSIBLE_PLAYBOOK", extraVars.get("ANSIBLE_PLAYBOOK", ""),
                            "Ansible playbook to be run remotely")
                stringParam("TAGS", extraVars.get("TAGS", ""),
                            "Tags on ansible playbook")
            }

            def access_control = extraVars.get('ACCESS_CONTROL',[])
            access_control.each { acl ->
                common_read_permissions.each { perm ->
                    authorization {
                        permission(perm,acl)
                    }
                }
            }

            def gitCredentialId = extraVars.get('SECURE_GIT_CREDENTIALS','')
            def additionalRepo = extraVars.get("ADDITIONAL_REPO", '')

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
                if (additionalRepo){
                    git{
                        remote{
                            url(additionalRepo)
                            branch(extraVars.get('ADDITIONAL_REPO_BRANCH', 'master'))
                            if (gitCredentialId) {
                                credentials(gitCredentialId)
                            }
                        }
                        extensions{
                            cleanAfterCheckout()
                            pruneBranches()
                            relativeTargetDirectory(extraVars.get('ADDITIONAL_REPO_TARGET_DIRECTORY',''))
                        }
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
                env('REGION', extraVars.get('REGION','us-east-1'))
                env('ANSIBLE_SSH_USER', extraVars.get('SSH_USER','ubuntu'))
            }

            steps {
                virtualenv {
                    pythonName('System-CPython-3.5')
                    name(jobName)
                    nature('shell')
                    command(dslFactory.readFileFromWorkspace('devops/resources/run-remote-ansible-playbook.sh'))
                }
            }

            publishers {
                mailer(extraVars.get('NOTIFY_ON_FAILURE',''), false, false)
            }
        }
    }
}

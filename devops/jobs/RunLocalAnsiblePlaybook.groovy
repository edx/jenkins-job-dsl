/*

    This job is useful when running a playbook locally (on jenkins) and not targetting
    remote machines.  As such, it does not require many of the standard arguments, such
    as an SSH identity.  It will default to checking out the 3 standard config repos (configuration,
    config-secure, config-internal) which means this job is not suitable for general consumption
    since it exposes edx-secure in the Workspace.

    Variables consumed from the EXTRA_VARS input to your seed job in addition
    to those listed in the seed job.

    CONFIGURATION_REPO: standard default available
    CONFIGURATION_BRANCH: defaults to master
    CONFIGURATION_SECURE_REPO: (required)
    CONFIGURATION_SECURE_BRANCH: defaults to master
    CONFIGURATION_INTERNAL_REPO: (required)
    CONFIGURATION_INTERNAL_BRANCH: defaults to master
    ACCESS_CONTROL: list of Jenkins matrix targets who should get read access (edx*team or username)
    TAGS: a list of tags to pass to the ansible-playbook invocation (optional)
    PLAYBOOK: name of the playbook to run, assumed to live in playbooks/edx-east/

*/
package devops.jobs

import static org.edx.jenkins.dsl.DevopsConstants.common_wrappers
import static org.edx.jenkins.dsl.DevopsConstants.common_logrotator
import static org.edx.jenkins.dsl.DevopsConstants.common_read_permissions
import static org.edx.jenkins.dsl.DevopsConstants.common_configuration_parameters
import static org.edx.jenkins.dsl.DevopsConstants.common_configuration_multiscm

class RunLocalAnsiblePlaybook {
    public static def job = { dslFactory, jobName, environment, deployment, playbook, extraVars, pre_ansible_steps = [], post_ansible_steps = [] ->
        return dslFactory.job(extraVars.get("FOLDER_NAME") + "/${jobName}") {

            wrappers common_wrappers

            logRotator common_logrotator

            parameters common_configuration_parameters(extraVars)

            def access_control = extraVars.get('ACCESS_CONTROL',[])
            access_control.each { acl ->
                common_read_permissions.each { perm ->
                    authorization {
                        permission(perm,acl)
                    }
                }
            }

            multiscm common_configuration_multiscm(extraVars)

            // These are passed in from the seed job for this specific instance of the seed job
            environmentVariables {
                env('PLAYBOOK', extraVars.get('PLAYBOOK'))
                env('TAGS', extraVars.get('TAGS',''))
                env('ENVIRONMENT', environment)
                env('DEPLOYMENT', deployment)
                env('ANSIBLE_EXTRA_VARS', extraVars.get('ANSIBLE_EXTRA_VARS',''))
            }

            pre_ansible_steps.each { pre_step ->
              steps pre_step(dslFactory)
            }

            steps {
                virtualenv {
                    name(jobName)
                    nature('shell')
                    command(dslFactory.readFileFromWorkspace('devops/resources/run-local-ansible-playbook.sh'))
                }
            }

            post_ansible_steps.each { post_step ->
              steps post_step(dslFactory)
            }

        }
    }

}

package devops.jobs

import static org.edx.jenkins.dsl.DevopsConstants.common_wrappers
import static org.edx.jenkins.dsl.DevopsConstants.common_logrotator

class RunAnsible {
    public static def job = { dslFactory, jobName, environment, deployment, extraVars ->
        return dslFactory.job(extraVars.get("FOLDER_NAME") + "/${environment}-${deployment}-${jobName}") {
            /*
               Run arbitrary remote commands on a host belonging to a target environment, deployment and cluster,
               in a specified region.
             */

            wrappers common_wrappers
                wrappers {
                    credentialsBinding {
                        file('AWS_CONFIG_FILE','jenkins-aws-credentials')
                        string('ROLE_ARN','ec2py-role-arn')
                    }
                    sshAgent(extraVars.get("SSH_AGENT_KEY").get(environment))
                }

            parameters {
                stringParam('CONFIGURATION_REPO', extraVars.get('CONFIGURATION_REPO', 'https://github.com/edx/configuration.git'),
                            'Git repo containing the analytics pipeline configuration automation.')
                stringParam('CONFIGURATION_BRANCH', extraVars.get('CONFIGURATION_BRANCH', 'master'),
                            'e.g. tagname or origin/branchname')
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
        }
    }
}

/*
 
 This job expects the following credentials to be defined on the folder
    tools-edx-jenkins-aws-credentials: file with key/secret in boto config format
    find-active-instances-${deployment}-role-arn: the role to aws sts assume-role

*/

package devops.jobs
import static org.edx.jenkins.dsl.Constants.common_wrappers
import static org.edx.jenkins.dsl.Constants.common_logrotator

class ClusterInstanceMonitoring{
	public static def job = { dslFactory, extraVars ->
		assert extraVars.containsKey('DEPLOYMENTS') : "Please define DEPLOYMENTS. It should be a list of strings."
        assert !(extraVars.get('DEPLOYMENTS') instanceof String) : "Make sure DEPLOYMENTS is a list and not a string"
		extraVars.get('DEPLOYMENTS').each { deployment ->
			
			dslFactory.job(extraVars.get("FOLDER_NAME","Monitoring") + "/cluster-instance-monitoring-${deployment}") {
				wrappers common_wrappers
                logRotator common_logrotator

                wrappers {
                    credentialsBinding {
                        file('AWS_CONFIG_FILE','tools-edx-jenkins-aws-credentials')
                        def role = "find-active-instances-${deployment}-role-arn"
                        string('ROLE_ARN', role)
                    }
                }

                def gitCredentialId = extraVars.get('SECURE_GIT_CREDENTIALS','')

                def config_internal_repo = "git@github.com:edx/${deployment}-internal.git"
                if (deployment == 'mckinsey'){
                	config_internal_repo = "git@github.com:mckinseyacademy/mckinsey-internal.git"
                }
                
                extraVars['CONFIGURATION_INTERNAL_REPO'] = config_internal_repo

                parameters{
                    stringParam('CONFIGURATION_REPO', extraVars.get('CONFIGURATION_REPO', 'https://github.com/edx/configuration.git'),
                                    'Git repo containing edX configuration.')
                    stringParam('CONFIGURATION_BRANCH', extraVars.get('CONFIGURATION_BRANCH', 'hchen/cluster-monitoring'),
                            'e.g. tagname or origin/branchname')

                    stringParam('CONFIGURATION_INTERNAL_REPO', extraVars.get('CONFIGURATION_INTERNAL_REPO',  "git@github.com:edx/${deployment}-internal.git"),
                    		'Git repo containing internal overrides')
            		stringParam('CONFIGURATION_INTERNAL_BRANCH', extraVars.get('CONFIGURATION_INTERNAL_BRANCH', 'hchen/cluster-monitoring'),
                    		'e.g. tagname or origin/branchname')
                }
                
                multiscm{
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

                    git {
                        remote {
                            url('$CONFIGURATION_INTERNAL_REPO')
                            branch('$CONFIGURATION_INTERNAL_BRANCH')
                                if (gitCredentialId) {
                                    credentials(gitCredentialId)
                                }
                        }
                        extensions {
                            cleanAfterCheckout()
                            pruneBranches()
                            relativeTargetDirectory('configuration-internal')
                        }
                    }
                }

                throttleConcurrentBuilds {
                    maxPerNode(0)
                    maxTotal(0)
                }

                triggers {
                    cron("H/10 * * * *")
                }

                environmentVariables {
                    env('REGION', extraVars.get('REGION','us-east-1'))
                }

                steps {
                    virtualenv {
                        nature("shell")
                        systemSitePackages(false)

                        command(
                            dslFactory.readFileFromWorkspace("devops/resources/cluster-instance-monitoring.sh")
                        )

                    }

                }

                publishers {
                    mailer(extraVars.get('NOTIFY_ON_FAILURE','devops@edx.org'), false, false)
                }

			}
		}
	}
}
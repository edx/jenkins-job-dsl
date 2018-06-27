/*
    Variables without defaults are marked (required)

    Variables consumed for this job:
        * DEPLOYMENTS: (required)
            deployment
                aws_region: region, default is us-east-1
                s3_log_bucket: name of bucket to log things being cleaned (required)
                aws_cleaner: space separated string of names of cleaner instances you want to execute on this deployment (required)
        * NOOP: boolean value to perform no operations, default is false
        * SYSADMIN_REPO: repository containing python script with cleaner instances (required)
        * SECURE_GIT_CREDENTIALS: secure-bot-user (required)
        * SYSADMIN_BRANCH: default is master
        * CONFIGURATION_REPO: name of config repo, default is https://github.com/edx/configuration.git
        * CONFIGURATION_BRANCH: default is master
        * NOTIFY_ON_FAILURE: alert@example.com
        * FOLDER_NAME: folder


    This job expects the following credentials to be defined on the folder
    tools-edx-jenkins-aws-credentials: file with key/secret in boto config format
    tools-jenkins-janitor-${deployment}-role-arn: the role to aws sts assume-role

*/
package devops.jobs
import static org.edx.jenkins.dsl.Constants.common_logrotator
import static org.edx.jenkins.dsl.Constants.common_wrappers


class Janitor {

    public static def job = { dslFactory, extraVars ->
        assert extraVars.containsKey('DEPLOYMENTS') : "Please define DEPLOYMENTS. It should be a list of strings."
        assert !(extraVars.get('DEPLOYMENTS') instanceof String) : "Make sure DEPLOYMENTS is a list and not a string"
        extraVars.get('DEPLOYMENTS').each { deployment, configuration ->
            dslFactory.job(extraVars.get("FOLDER_NAME","Monitoring") + "/janitor-${deployment}") {

                logRotator common_logrotator
                wrappers common_wrappers

                wrappers {
                    credentialsBinding {
                        file('AWS_CONFIG_FILE','tools-edx-jenkins-aws-credentials')
                        string('ROLE_ARN', "tools-jenkins-janitor-${deployment}-role-arn")
                    }
                }

                assert extraVars.containsKey('SYSADMIN_REPO') : 'Please define a system administration repo where the janitor script is located.'

                def gitCredentialId = extraVars.get('SECURE_GIT_CREDENTIALS','')

                parameters{
                    stringParam('CONFIGURATION_REPO', extraVars.get('CONFIGURATION_REPO', 'https://github.com/edx/configuration.git'),
                            'Git repo containing edX configuration.')
                    stringParam('CONFIGURATION_BRANCH', extraVars.get('CONFIGURATION_BRANCH', 'master'),
                            'e.g. tagname or origin/branchname')
                    stringParam('SYSADMIN_REPO', extraVars.get('SYSADMIN_REPO'),
                            'Git repo containing sysadmin configuration which contains the janitor script.')
                    stringParam('SYSADMIN_BRANCH', extraVars.get('SYSADMIN_BRANCH', 'master'),
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
                            url('$SYSADMIN_REPO')
                            branch('$SYSADMIN_BRANCH')
                            if (gitCredentialId) {
                                credentials(gitCredentialId)
                            }
                        }
                        extensions {
                            cleanAfterCheckout()
                            pruneBranches()
                            relativeTargetDirectory('sysadmin')
                        }
                    }
                }

                triggers {
                    cron('H H/4 * * * ')
                }

                assert configuration.containsKey('aws_cleaner') : 'Please definine aws_cleaner for this configuration. It should be a string.'
                assert configuration.get('aws_cleaner') instanceof String : 'Make sure that aws_cleaner is a string. Different cleaner instanes should be separated by spaces.'
                assert configuration.containsKey('s3_log_bucket') : 'Please define an s3_log_bucket for this configuration.'

                environmentVariables {
                    env('S3_LOG_BUCKET', configuration.get('s3_log_bucket'))
                    env('AWS_REGION', configuration.get('aws_region', 'us-east-1'))
                    env('AWS_CLEANER', configuration.get('aws_cleaner'))
                    env('NOOP', extraVars.get('NOOP', false))
                }


                steps {
                    virtualenv {
                        nature("shell")
                        systemSitePackages(false)

                        command(
                            dslFactory.readFileFromWorkspace("devops/resources/janitor.sh")
                        )
                    }
                }

                if (extraVars.get('NOTIFY_ON_FAILURE')){
                    publishers {
                        mailer(extraVars.get('NOTIFY_ON_FAILURE'), false, false)
                    }
                }



            }
        }
    }

}

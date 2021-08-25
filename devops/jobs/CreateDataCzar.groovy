/*
 Create Data Czar

 Variables without defaults are marked (required) 
 
 Variables consumed for this job:
    * SECURE_GIT_CREDENTIALS: secure-bot-user (required)
    * NOTIFY_ON_FAILURE: alert@example.com
    * FOLDER_NAME: folder
    * SYS_ADMIN_REPO: repo where the mongo backup script is located (required)
    * STATUS_BRANCH: default is master
    * ORGANIZATION: organization name to create data czar for
    * GPG_KEY: Key required for encryptring aws credentials
    This job expects the following credentials to be defined on the folder
    tools-edx-jenkins-aws-credentials: file with key/secret in boto config format
    

*/
package devops.jobs
import static org.edx.jenkins.dsl.Constants.common_logrotator
import static org.edx.jenkins.dsl.Constants.common_wrappers

class CreateDataCzar{
    public static def job = { dslFactory, extraVars ->
        dslFactory.job(extraVars.get("FOLDER_NAME","Data Czar") + "/create-data-czar") {

            logRotator common_logrotator
            wrappers common_wrappers

            wrappers{
                credentialsBinding{
                    file('AWS_CONFIG_FILE','tools-edx-jenkins-aws-credentials')
                    string('ROLE_ARN', "create-data-czar-edx-role-arn")
                }
            }

            parameters{
                stringParam('CONFIGURATION_REPO', extraVars.get('CONFIGURATION_REPO', 'https://github.com/edx/configuration.git'),
                            'Git repo containing edX configuration.')
                stringParam('CONFIGURATION_BRANCH', extraVars.get('CONFIGURATION_BRANCH', 'master'),
                        'e.g. tagname or origin/branchname')
                stringParam('ORGANIZATION',
                        'Name of organization to create data czar. e.g alaskax')
                booleanParam('CREATE_ORG', true,
                        'true if create new organization or false if organization is already created.')
                stringParam('USER_EMAIL',
                        'User Email address to generate Data Czar')
                fileParam('user_gpg_key.gpg', 
                        'Paste the GPG key for encryption of aws credentials')
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
            }


            steps {
               shell(dslFactory.readFileFromWorkspace('devops/resources/create-data-czar.sh'))
            }

            publishers {
                archiveArtifacts {
                    pattern('sysadmin/create_data_czar/*-credentials.txt.gpg')
                    onlyIfSuccessful()
                }
                if (extraVars.get('NOTIFY_ON_FAILURE')){
                    extendedEmail {
                        recipientList(extraVars.get('NOTIFY_ON_FAILURE'))
                        triggers {
                            failure {
                                attachBuildLog(false)
                                compressBuildLog(false)
                                subject('Failed build: ${JOB_NAME} #${BUILD_NUMBER}')
                                content('Jenkins job: ${JOB_NAME} failed. \nFor ${ORGANIZATION} Organization. \nFor ${USER_EMAIL} User Email. \n\nSee ${BUILD_URL} for details.')
                                contentType('text/plain')
                                sendTo {
                                    recipientList()
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

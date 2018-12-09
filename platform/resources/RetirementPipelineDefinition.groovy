/*
 * Main user retirement pipeline
 */
pipeline {
    agent {
        // By default, run all the stages on standard jenkins-worker instances.
        // For now, this means we may launch a lot of jenkins-worker instances
        // per week.  Also the idea of a retirement stage agent being reused
        // for a public build worker seems like a bad idea for security.
        // PLAT-2096 will attempt to address this.
        label "jenkins-worker"
    }
    parameters {
        string(
            name: 'environment',
            defaultValue: '',  // caller must specify environment
            description: 'The environment-deployment to retire the user from'
        )
        string(name: 'original_username', defaultValue: '', description: 'The original username of the user to retire.')
        string(name: 'original_email', defaultValue: '', description: 'The original email address of the user to retire.')
        string(name: 'retired_username', defaultValue: '', description: 'The retired username of the user to retire, containing the hash.')
        string(name: 'retired_email', defaultValue: '', description: 'The retired email address of the user to retire, containing the hash.')

        // The following will allow us to run steps in isolation, re-run steps that
        // have failed in the past, exclude known failing steps, etc.
        booleanParam(name: 'do_lms_lock_account', defaultValue: true, description: 'Do the LMS lock account step.')
        booleanParam(name: 'do_credentials', defaultValue: true, description: 'Do the credentials retirement step.')
        booleanParam(name: 'do_ecom', defaultValue: true, description: 'Do the e-commerce retirement step.')
        booleanParam(name: 'do_lms_forums', defaultValue: true, description: 'Do the LMS forums retirement step.')
        booleanParam(name: 'do_lms_email_lists', defaultValue: true, description: 'Do the LMS email lists opt-out step.')
        booleanParam(name: 'do_lms_unenroll', defaultValue: true, description: 'Do the LMS course unenroll step.')
        booleanParam(name: 'do_lms_notes', defaultValue: true, description: 'Do the LMS notes retirement step.')
    }
    environment {
        // This pipeline assumes the following structure for any secrets YAML file:
        //
        //     ---
        //     prod-edx:
        //         lms:
        //             base_url: https://courses.edx.org/
        //             client_id: foo
        //             client_secret: bar
        //         ecommerce:
        //             ...
        //         ...
        //     stage-edx:
        //         ...
        //     ...
        //
        // In short, the heirarchy is ENVIRONMENT > IDA > SECRET TYPE.
        //
        // For now, use "overrides" in the credential ID to make it easier to
        // transition to reading from a credentials repo and using this as
        // overrides only.
        RETIREMENT_SECRETS = credentials('retirement_driver_secrets_overrides.yml')
    }
    stages {
        stage('LMS - lock account') {
            when { expression { return params.do_lms_lock_account } }
            steps {
                sh 'echo "running the LMS - lock account stage"'
                script {
                    retirement_driver_secrets = readYaml file: "$RETIREMENT_SECRETS"
                    lms_secrets = retirement_driver_secrets.get(params.environment).lms
                }
                // TODO: PLAT-2047
                // Make use of the following values:
                //   lms_secrets.base_url
                //   lms_secrets.client_id
                //   lms_secrets.client_secret
            }
        }
        stage('CREDENTIALS - retire user') {
            when { expression { return params.do_credentials } }
            steps {
                sh 'echo "running the CREDENTIALS - retire user stage"'
                script {
                    retirement_driver_secrets = readYaml file: "$RETIREMENT_SECRETS"
                    credentials_secrets = retirement_driver_secrets.get(params.environment).credentials
                }
                // TODO: PLAT-2053.  Make use of the following values:
                //   credentials_secrets.base_url
                //   credentials_secrets.client_id
                //   credentials_secrets.client_secret
            }
        }
        stage('ECOM - retire user') {
            when { expression { return params.do_ecom } }
            steps {
                sh 'echo "running the ECOM - retire user stage"'
                script {
                    retirement_driver_secrets = readYaml file: "$RETIREMENT_SECRETS"
                    ecommerce_secrets = retirement_driver_secrets.get(params.environment).ecommerce
                }
                // TODO: PLAT-2052.  Make use of the following values:
                //   ecom_secrets.base_url
                //   ecom_secrets.client_id
                //   ecom_secrets.client_secret
            }
        }
        stage('LMS - forums retirement') {
            when { expression { return params.do_lms_forums } }
            steps {
                sh 'echo "running the LMS - forums retirement stage"'
                script {
                    retirement_driver_secrets = readYaml file: "$RETIREMENT_SECRETS"
                    lms_secrets = retirement_driver_secrets.get(params.environment).lms
                }
                // TODO: PLAT-2049. Make use of the following values:
                //   lms_secrets.base_url
                //   lms_secrets.client_id
                //   lms_secrets.client_secret
            }
        }
        stage('LMS - email list opt-out') {
            when { expression { return params.do_lms_email_lists } }
            steps {
                sh 'echo "running the LMS - email list opt-out stage"'
                script {
                    retirement_driver_secrets = readYaml file: "$RETIREMENT_SECRETS"
                    lms_secrets = retirement_driver_secrets.get(params.environment).lms
                }
                // TODO: PLAT-2048. Make use of the following values:
                //   lms_secrets.base_url
                //   lms_secrets.client_id
                //   lms_secrets.client_secret
            }
        }
        stage('LMS - unenroll') {
            when { expression { return params.do_lms_unenroll } }
            steps {
                sh 'echo "running the LMS - unenroll stage"'
                script {
                    retirement_driver_secrets = readYaml file: "$RETIREMENT_SECRETS"
                    lms_secrets = retirement_driver_secrets.get(params.environment).lms
                }
                // TODO: PLAT-2050. Make use of the following values:
                //   lms_secrets.base_url
                //   lms_secrets.client_id
                //   lms_secrets.client_secret
            }
        }
    }
}

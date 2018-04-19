/*
 * Main user retirement pipeline
 */
pipeline {
    agent {
        // Try to run on standard jenkins-worker instances, otherwise the
        // backup-runner will suffice as fallback.  Master is currently
        // designated as a backup-runner.
        label 'jenkins-worker'
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
    stages {
        stage('Checkout') {
            steps {
                // checkout secure repo to get secure defaults such as API auth client keys
                sh 'echo "running the Checkout stage"'

            }
        }
        stage('LMS - lock account') {
            when { expression { return params.do_lms_lock_account } }
            steps {
                sh 'echo "running the LMS - lock account stage"'
                // TODO: PLAT-2047
            }
        }
        stage('CREDENTIALS - retire user') {
            when { expression { return params.do_credentials } }
            steps {
                sh 'echo "running the CREDENTIALS - retire user stage"'
                // TODO: PLAT-2053
            }
        }
        stage('ECOM - retire user') {
            when { expression { return params.do_ecom } }
            steps {
                sh 'echo "running the ECOM - retire user stage"'
                // TODO: PLAT-2052
            }
        }
        stage('LMS - forums retirement') {
            when { expression { return params.do_lms_forums } }
            steps {
                sh 'echo "running the LMS - forums retirement stage"'
                // TODO: PLAT-2049
            }
        }
        stage('LMS - email list opt-out') {
            when { expression { return params.do_lms_email_lists } }
            steps {
                sh 'echo "running the LMS - email list opt-out stage"'
                // TODO: PLAT-2048
            }
        }
        stage('LMS - unenroll') {
            when { expression { return params.do_lms_unenroll } }
            steps {
                sh 'echo "running the LMS - unenroll stage"'
                // TODO: PLAT-2050
            }
        }
    }
}

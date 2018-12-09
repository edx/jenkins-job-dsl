# Install a test course
ssh ${SSH_USER}@${SANDBOX_HOST} -o StrictHostKeyChecking=no "sudo su edxapp -s /bin/bash -c 'source /edx/app/edxapp/edxapp_env && /edx/app/edxapp/venvs/edxapp/src/edx-ora2/scripts/install-test-course.sh'"

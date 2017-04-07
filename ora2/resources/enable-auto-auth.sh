# Enable auto auth for a sandbox
SANDBOX_HOST=${SANDBOX_BASE}.sandbox.edx.org
ssh ${SSH_USER}@${SANDBOX_HOST} -o StrictHostKeyChecking=no "sudo sed -i 's/\"AUTOMATIC_AUTH_FOR_TESTING\": false/\"AUTOMATIC_AUTH_FOR_TESTING\": true/' /edx/app/edxapp/cms.env.json && sudo sed -i 's/\"AUTOMATIC_AUTH_FOR_TESTING\": false/\"AUTOMATIC_AUTH_FOR_TESTING\": true/' /edx/app/edxapp/lms.env.json && sudo /edx/bin/supervisorctl restart edxapp:*"

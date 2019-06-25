#!/bin/bash

cd $WORKSPACE/configuration/util/jenkins/primary_keys

pip install -r requirements.txt
. ../assume-role.sh

# Assume role for different envs
set +x
assume-role ${ROLE_ARN}
set -x

# Set RDSIGNORE if not set in job, need because we're setting -u
# Otherwise we get an error "RDSIGNORE: unbound variable"
if [[ ! -v RDSIGNORE ]]; then
    RDSIGNORE=""
fi

python ./check_primary_keys.py --environment ${ENVIRONMENT} --deploy ${DEPLOYMENT} --region $AWS_DEFAULT_REGION --recipient $TO_ADDRESS --sender $FROM_ADDRESS ${RDSIGNORE}

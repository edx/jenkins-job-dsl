#!/bin/bash

cd $WORKSPACE/configuration/util/jenkins/export_slow_logs

pip install -r requirements.txt
. ../assume-role.sh

# Assume the role
set +x
assume-role ${ROLE_ARN}
set -x

# Set RDSIGNORE if not set in job, need because we're setting -u
# Otherwise we get an error "RDSIGNORE: unbound variable"
if [[ ! -v RDSIGNORE ]]; then
    RDSIGNORE=""
fi

python export_slow_query_logs.py --environment ${ENVIRONMENT} ${RDSIGNORE}

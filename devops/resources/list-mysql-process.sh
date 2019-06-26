#!/bin/bash

cd $WORKSPACE/configuration/util/jenkins/list_mysql_process

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

python list_mysql_process.py --environment ${ENVIRONMENT} ${RDSIGNORE}

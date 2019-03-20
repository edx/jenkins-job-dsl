#!/bin/bash

cd $WORKSPACE/configuration/util/jenkins/check_table_size

pip install -r requirements.txt
. ../assume-role.sh

# Assume the role
set +x
assume-role ${ROLE_ARN}
set -x

# Set RDSTHRESHOLD if not set in job, need because we're setting -u
# Otherwise we get an error "RDSTHRESHOLD: unbound variable"
if [[ ! -v RDSTHRESHOLD ]]; then
    RDSTHRESHOLD=""
fi

python check_table_size.py  --username ${USERNAME} --password ${PASSWORD} --threshold ${THRESHOLD} ${RDSTHRESHOLD}

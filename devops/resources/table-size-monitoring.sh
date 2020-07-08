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

# Set RDSIGNORE if not set in job, need because we're setting -u
# Otherwise we get an error "RDSIGNORE: unbound variable"
if [[ ! -v RDSIGNORE ]]; then
    RDSIGNORE=""
fi

python check_table_size.py  --threshold ${THRESHOLD} ${RDSTHRESHOLD} ${RDSIGNORE}

curl -X GET 'https://api.opsgenie.com/v2/heartbeats/table-size-monitoring-'${DEPLOYMENT}'/ping' -H 'Authorization: GenieKey '${GENIE_KEY}

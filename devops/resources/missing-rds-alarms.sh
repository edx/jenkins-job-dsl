#!/bin/bash

cd $WORKSPACE/configuration/util/jenkins/rds_alarms_checker

pip install -r requirements.txt
. ../assume-role.sh

# Assume the role
set +x
assume-role ${ROLE_ARN}
set -x

if [[ ! -v WHITELIST ]]; then
    WHITELIST=""
fi

python missing_rds_alarms.py ${WHITELIST}


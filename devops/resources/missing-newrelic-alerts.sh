#!/bin/bash

cd $WORKSPACE/configuration/util/jenkins/missing_alerts_checker

pip install -r requirements.txt
. ../assume-role.sh

# Assume the role
set +x
assume-role ${ROLE_ARN}
set -x

# Set IGNORE_LIST if not set in job, need because we're setting -u
# Otherwise we get an error "IGNORE_LIST: unbound variable"
if [[ ! -v IGNORE_LIST ]]; then
    IGNORE_LIST=""
fi

python missing_alerts_checker.py --new-relic-api-key ${NEW_RELIC_API_KEY} ${IGNORE_LIST}

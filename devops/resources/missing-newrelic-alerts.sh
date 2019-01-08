#!/bin/bash

cd $WORKSPACE/configuration/util/jenkins/

pip install -r check_celery_progress/requirements.txt
. ./assume-role.sh

# Assume the role
set +x
assume-role ${ROLE_ARN}
set -x

python missing_alerts_checker.py --new-relic-api-key ${NEW_RELIC_API_KEY}

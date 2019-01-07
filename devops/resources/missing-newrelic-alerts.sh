#!/bin/bash

cd $WORKSPACE/configuration/util/jenkins/

pip install -r check_celery_progress/requirements.txt

. util/jenkins/assume-role.sh
# Assume the role
assume-role ${ROLE_ARN}

python missing_alerts_checker.py --new-relic-api-key ${NEW_RELIC_API_KEY}

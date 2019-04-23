#!/bin/bash

cd $WORKSPACE/configuration/util/jenkins/export_slow_logs

pip install -r requirements.txt
. ../assume-role.sh

# Assume the role
set +x
assume-role ${ROLE_ARN}
set -x

python export_slow_query_logs.py --environment ${ENVIRONMENT}

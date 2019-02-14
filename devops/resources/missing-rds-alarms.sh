#!/bin/bash

cd $WORKSPACE/configuration

pip install -r requirements.txt
. util/jenkins/assume-role.sh

# Assume the role
set +x
assume-role ${ROLE_ARN}
set -x

python util/jenkins/missing_rds_alarms.py

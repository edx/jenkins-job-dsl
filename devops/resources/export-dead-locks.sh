#!/bin/bash

cd $WORKSPACE/configuration/util/jenkins/export_dead_locks

pip install -r requirements.txt
. ../assume-role.sh

# Assume the role
set +x
assume-role ${ROLE_ARN}
set -x

python export_dead_locks.py --environment ${ENVIRONMENT} --hostname ${HOSTNAME} --port ${PORT} --indexname ${INDEXNAME}

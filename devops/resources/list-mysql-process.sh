#!/bin/bash

cd $WORKSPACE/configuration/util/jenkins/list_mysql_process

pip install -r requirements.txt
. ../assume-role.sh

# Assume the role
set +x
assume-role ${ROLE_ARN}
set -x

python list_mysql_process.py --environment ${ENVIRONMENT}

#!/bin/bash

cd $WORKSPACE/configuration/util/jenkins/check_table_size

pip install -r requirements.txt
. ../assume-role.sh

# Assume the role
set +x
assume-role ${ROLE_ARN}
set -x

python check_primary_keys.py

#!/bin/bash

cd $WORKSPACE/configuration/util/jenkins/primary_keys

pip install -r requirements.txt
. ../assume-role.sh

# Assume the role
set +x
assume-role ${ROLE_ARN}
set -x

python check_primary_keys.py

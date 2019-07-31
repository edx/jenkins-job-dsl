#!/bin/bash
set -exuo pipefail

# Required by click http://click.pocoo.org/5/python3/
export LC_ALL=C.UTF-8
export LANG=C.UTF-8

cd $WORKSPACE/configuration/util/jenkins/add_new_xqueues_to_dashboard
pip install -r requirements.txt
env

. ../assume-role.sh
assume-role ${ROLE_ARN}
python ./add_xqueue_to_dashboard.py --environment ${ENVIRONMENT} --deploy ${DEPLOYMENT}

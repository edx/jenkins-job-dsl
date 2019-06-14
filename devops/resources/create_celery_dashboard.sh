#!/bin/bash
set -exuo pipefail

# Required by click http://click.pocoo.org/5/python3/
export LC_ALL=C.UTF-8
export LANG=C.UTF-8

cd $WORKSPACE/configuration/util/jenkins/update_celery_monitoring
pip install -r requirements.txt
env

. ../assume-role.sh
assume-role ${ROLE_ARN}
python ./create_celery_dashboard.py --environment ${ENVIRONMENT} --deploy ${DEPLOYMENT}

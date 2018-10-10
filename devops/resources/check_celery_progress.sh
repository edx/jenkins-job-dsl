#!/bin/bash
set -exuo pipefail

# Required by click http://click.pocoo.org/5/python3/
export LC_ALL=C.UTF-8
export LANG=C.UTF-8

cd $WORKSPACE/configuration/util/jenkins/check_celery_progress
pip install -r requirements.txt
env

# Set THRESHOLDS if not set in job, need because we're setting -u
# Otherwise we get an error "THRESHOLDS: unbound variable"
if [[ ! -v THRESHOLDS ]]; then
    THRESHOLDS=""
fi

. ./assume-role.sh
assume-role ${ROLE_ARN}
python ./check_celery_progress.py --environment ${ENVIRONMENT} --deploy ${DEPLOYMENT} --host ${REDIS_HOST} --sns-arn ${SNS_TOPIC} ${THRESHOLDS}

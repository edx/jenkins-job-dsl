#!/bin/bash
set -exuo pipefail

cd $WORKSPACE/configuration/util/jenkins
pip install -r requirements-celery.txt
env

# Set THRESHOLDS if not set in job, need because we're setting -u
# Otherwise we get an error "THRESHOLDS: unbound variable"
if [[ ! -v THRESHOLDS ]]; then
    THRESHOLDS=""
fi

. ./assume-role.sh
assume-role ${ROLE_ARN}
python ./check-celery-queues.py --environment ${ENVIRONMENT} --deploy ${DEPLOYMENT} --host ${REDIS_HOST} --sns-arn ${SNS_TOPIC} ${THRESHOLDS}

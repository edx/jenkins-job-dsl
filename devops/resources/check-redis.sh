#!/bin/bash
set -exuo pipefail

cd $WORKSPACE/configuration/util/jenkins
pip install -r requirements-celery.txt
env

. ./assume-role.sh
assume-role ${ROLE_ARN}
python ./check-celery-queues.py --environment ${ENVIRONMENT} --deploy ${DEPLOYMENT} --host ${REDIS_HOST} --sns-arn ${SNS_TOPIC}

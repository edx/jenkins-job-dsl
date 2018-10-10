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

python ./check_celery_progress.py --ops-genie ${OPSGENIE_API_KEY} --environment ${ENVIRONMENT} --deploy ${DEPLOYMENT} --host ${REDIS_HOST} ${THRESHOLDS}

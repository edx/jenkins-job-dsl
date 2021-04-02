#!/bin/bash
set -exuo pipefail

set +u
. /edx/var/jenkins/jobvenvs/virtualenv_tools.sh
# creates a venv with its location stored in variable "venvpath"
create_virtualenv --python=python3.8 --clear
. "$venvpath/bin/activate"
set -u

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

. ../assume-role.sh
assume-role ${ROLE_ARN}
python ./check_celery_progress.py --opsgenie-api-key ${OPSGENIE_API_KEY} --environment ${ENVIRONMENT} --deploy ${DEPLOYMENT} --host ${REDIS_HOST} ${THRESHOLDS}

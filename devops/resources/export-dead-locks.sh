#!/bin/bash

set +u
. /edx/var/jenkins/jobvenvs/virtualenv_tools.sh
# creates a venv with its location stored in variable "venvpath"
create_virtualenv --python=python3.8 --clear
. "$venvpath/bin/activate"
set -u

cd $WORKSPACE/configuration/util/jenkins/export_dead_locks

pip install -r requirements.txt
. ../assume-role.sh

# Assume the role
set +x
assume-role ${ROLE_ARN}
set -x

# Set RDSIGNORE if not set in job, need because we're setting -u
# Otherwise we get an error "RDSIGNORE: unbound variable"
if [[ ! -v RDSIGNORE ]]; then
    RDSIGNORE=""
fi

python export_dead_locks.py --environment ${ENVIRONMENT} --hostname ${HOSTNAME} --port ${PORT} --indexname ${INDEXNAME} ${RDSIGNORE}

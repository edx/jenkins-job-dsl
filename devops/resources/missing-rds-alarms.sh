#!/bin/bash

set +u
. /edx/var/jenkins/jobvenvs/virtualenv_tools.sh
# creates a venv with its location stored in variable "venvpath"
create_virtualenv --python=python3.8 --clear
. "$venvpath/bin/activate"
set -u

cd $WORKSPACE/configuration/util/jenkins/rds_alarms_checker

pip install -r requirements.txt
. ../assume-role.sh

# Assume the role
set +x
assume-role ${ROLE_ARN}
set -x

if [[ ! -v IGNORE_OPTIONS ]]; then
    IGNORE_OPTIONS=""
fi

python missing_rds_alarms.py ${IGNORE_OPTIONS}


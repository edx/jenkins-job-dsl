#!/bin/bash

set +u
. /edx/var/jenkins/jobvenvs/virtualenv_tools.sh
# creates a venv with its location stored in variable "venvpath"
create_virtualenv --python=python3.8 --clear
. "$venvpath/bin/activate"
set -u

set -x 

cd $WORKSPACE/configuration

. util/jenkins/assume-role.sh
assume-role ${ROLE_ARN}

cd $WORKSPACE/configuration/util/check_rds_slow_query_logs

pip install -r requirements.txt

if [[ ! -v IGNORE_OPTIONS ]]; then
    IGNORE_OPTIONS=""
fi

python check_rds_configs.py --db_engine mysql ${IGNORE_OPTIONS}

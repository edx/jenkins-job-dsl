#!/bin/bash

set +u
. /edx/var/jenkins/jobvenvs/virtualenv_tools.sh
# creates a venv with its location stored in variable "venvpath"
create_virtualenv --python=python3.8 --clear
. "$venvpath/bin/activate"
set -u

cd $WORKSPACE/configuration/util/jenkins/retired_user_cert_remover

pip install -r requirements.txt
. ../assume-role.sh

# Assume role for different envs
set +x
assume-role ${ROLE_ARN}
set -x

python ./retired_user_cert_remover.py -h "${ENVIRONMENT}-${DEPLOYMENT}-edxapp.rds.edx.org" -db wwc --dry-run

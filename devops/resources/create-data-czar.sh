#!/bin/bash
set -exuo pipefail

set +u
. /edx/var/jenkins/jobvenvs/virtualenv_tools.sh
# creates a venv with its location stored in variable "venvpath"
create_virtualenv --python=python3.8 --clear
. "$venvpath/bin/activate"
set -u

cd $WORKSPACE/configuration
pip install -r requirements.txt
. util/jenkins/assume-role.sh

assume-role ${ROLE_ARN}

cd $WORKSPACE/sysadmin
pip install -r requirements.txt
cd create_data_czar

# Create Policy
python ./create_org_data_czar_policy.py --org ${ORGANIZATION}

# Create User and add to group
python ./create_data_czar.py --user ${USER_EMAIL} --file $WORKSPACE/user_gpg_key.gpg --org ${ORGANIZATION}
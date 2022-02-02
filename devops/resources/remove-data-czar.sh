#!/bin/bash
set -exuo pipefail

set +u
. /edx/var/jenkins/jobvenvs/virtualenv_tools.sh
# creates a venv with its location stored in variable "venvpath"
create_virtualenv --python=python3.8 --clear
. "$venvpath/bin/activate"
set -u

cd "$WORKSPACE/configuration"
pip install -r util/jenkins/requirements.txt
. util/jenkins/assume-role.sh

assume-role ${ROLE_ARN}

cd util/create_data_czar

# Remove Data Czar
python ./remove_data_czar.py --user ${USER_EMAIL}

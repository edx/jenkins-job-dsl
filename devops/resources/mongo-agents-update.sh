#!/bin/bash

set +u
. /edx/var/jenkins/jobvenvs/virtualenv_tools.sh
# creates a venv with its location stored in variable "venvpath"
create_virtualenv --python=python3.8 --clear
. "$venvpath/bin/activate"
set -u

set -x

cd $WORKSPACE/configuration/
pip install -r requirements.txt

. util/jenkins/assume-role.sh

set +x
assume-role ${ROLE_ARN}
set -x
cd playbooks

ansible-playbook -i ./ec2.py --limit tag_Name_edx-admin-mms mongo_mms.yml -e@../../configuration-internal/ansible/vars/edx.yml -e@../../configuration-secure/ansible/vars/edx.yml -u ubuntu

#!/bin/bash

set -x

cd $WORKSPACE/configuration/
pip install -r requirements.txt

. util/jenkins/assume-role.sh

set +x
assume-role ${ROLE_ARN}
set -x
cd playbooks

ansible-playbook -i ./ec2.py --limit tag_Name_edx-admin-mms mongo_mms.yml -e@../../configuration-internal/ansible/vars/edx.yml -e@../../configuration-secure/ansible/vars/edx.yml -u ubuntu

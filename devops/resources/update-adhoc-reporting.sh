#!/bin/bash

set -exo pipefail

set +u
. /edx/var/jenkins/jobvenvs/virtualenv_tools.sh
# creates a venv with its location stored in variable "venvpath"
create_virtualenv --python=python3.8 --clear
. "$venvpath/bin/activate"
set -u

env

cd $WORKSPACE/configuration

pip install -r requirements.txt
. util/jenkins/assume-role.sh

set +x
assume-role ${ROLE_ARN}
set -x

cd playbooks

environments=(stage prod)

#update report replica for stage and prod edx
for environment in ${environments[@]}; do
    ansible-playbook -i ./ec2.py --limit tag_Name_tools-edx-gp tools-gp.yml -e@../../edx-internal/ansible/vars/edx.yml -e@../../edx-internal/ansible/vars/${environment}-edx.yml \
        -e@../../edx-secure/ansible/vars/edx.yml -e@../../edx-secure/ansible/vars/${environment}-edx.yml -e@../../edx-internal/ansible/vars/ad_hoc_reporting_replica_db_hosts.yml \
        --tags install:code -u ${ANSIBLE_SSH_USER} -D
done

# update report replica for prod edge
ansible-playbook -i ./ec2.py --limit tag_Name_tools-edx-gp tools-gp.yml -e@../../edge-internal/ansible/vars/edge.yml -e@../../edge-internal/ansible/vars/prod-edge.yml \
    -e@../../edge-secure/ansible/vars/edge.yml -e@../../edge-secure/ansible/vars/prod-edge.yml -e@../../edx-internal/ansible/vars/ad_hoc_reporting_replica_db_hosts.yml \
    --tags install:code -u ${ANSIBLE_SSH_USER} -D

# update users on reporting server

ansible-playbook -i ./ec2.py --limit tag_Name_tools-edx-gp tools-gp.yml --tags users -e@../../edx-secure/ansible/vars/edx.yml -e@../../edx-internal/ansible/vars/ad_hoc_reporting_users.yml -u ${ANSIBLE_SSH_USER}

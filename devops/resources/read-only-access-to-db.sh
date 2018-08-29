#!/bin/bash
set -exuo pipefail

cd $WORKSPACE/configuration

pip install -r requirements.txt

cd playbooks
ansible-playbook -i "$BASTION_HOST," tools-gp.yml -u ubuntu -e@../../configuration-secure/ansible/vars/users.yml --tags users 

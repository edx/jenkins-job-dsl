#!/bin/bash
set -exuo pipefail

cd $WORKSPACE/configuration
pip install -r requirements.txt
env
ansible-playbook -u ubuntu -i 'tools-edx-gp.edx.org,' playbooks/edx-east/tools-gp.yml --tags users -e "@../configuration-secure/ansible/vars/users.yml"

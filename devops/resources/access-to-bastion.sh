#!/bin/bash
set -exuo pipefail

cd $WORKSPACE/configuration

pip install -r requirements.txt

cd playbooks
ansible-playbook -i "$BASTION_HOST," tools-gp.yml -u ubuntu -e${USERS_YAML} -e@../../configuration-internal/ansible/vars/${DEPLOYMENT}.yml -e../../configuration-internal/ansible/vars/${DEPLOYMENT}-${ENVIRONMENT}.yml --tags users

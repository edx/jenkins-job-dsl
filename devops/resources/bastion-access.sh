#!/bin/bash
set -exuo pipefail

cd $WORKSPACE/configuration

pip install -r requirements.txt

cd playbooks
if [[ "${ENVIRONMENT}" = "qa" ]]; then
  ansible-playbook -i "$BASTION_HOST," tools-gp.yml -u ubuntu -e@../../configuration-internal/ansible/vars/${DEPLOYMENT}.yml -e@../../configuration-internal/ansible/vars/${ENVIRONMENT}-${DEPLOYMENT}.yml --tags users

else
  ansible-playbook -i "$BASTION_HOST," tools-gp.yml -u ubuntu -e${USERS_YAML} -e@../../configuration-internal/ansible/vars/${DEPLOYMENT}.yml -e@../../configuration-internal/ansible/vars/${ENVIRONMENT}-${DEPLOYMENT}.yml --tags users
fi

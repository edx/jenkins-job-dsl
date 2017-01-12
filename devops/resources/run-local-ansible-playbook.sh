#!/usr/bin/env bash

set -ex

cd $WORKSPACE/configuration

pip install -r requirements.txt

cd $WORKSPACE/configuration/playbooks/edx-east

if [[ -n ${PLAYBOOK} ]]; then
    ANSIBLE_PLAYBOOK=${PLAYBOOK}
else
    echo "You must specify PLAYBOOK"
    exit 1
fi

if [[ -n ${TAGS} ]]; then
  ANSIBLE_TAGS="--tags ${TAGS}"
else
  ANSIBLE_TAGS=""
fi

ansible-playbook -vvvv -i 127.0.0.1, -c local ${ANSIBLE_PLAYBOOK} -U $(whoami) \
-e@$WORKSPACE/configuration-internal/ansible/vars/${DEPLOYMENT}.yml \
-e@$WORKSPACE/configuration-internal/ansible/vars/${ENVIRONMENT}-${DEPLOYMENT}.yml \
-e@$WORKSPACE/configuration-secure/ansible/vars/${DEPLOYMENT}.yml \
-e@$WORKSPACE/configuration-secure/ansible/vars/${ENVIRONMENT}-${DEPLOYMENT}.yml \
${ANSIBLE_EXTRA_VARS} \
${ANSIBLE_TAGS}

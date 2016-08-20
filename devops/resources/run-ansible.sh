#!/usr/bin/env bash

set -e

cd $WORKSPACE/configuration
pip install -r requirements.txt
pip install awscli
export AWS_PROFILE=$deployment_tag

env

. util.sh

# Assume the role that will allow running ec2.py for getting a dynamic inventory
assume-role ${ROLE_ARN}

cd $WORKSPACE/configuration/playbooks

# Pattern must be supplied explicitly as we take a conservative
# approach given that ec2.py will provide a dynamic inventor
if [[ -z "${PATTERN}" ]]; then
  ANSIBLE_PATTERN="-i ${PATTERN}"
else
  ANSIBLE_PATTERN="__NONE__"
fi

if [[ -z "${INVENTORY}" ]]; then
  ANSIBLE_INVENTORY="-i ${INVENTORY} "
else
  ANSIBLE_INVENTORY="-i ./ec2.py"
fi

if [[ -z "${BECOME_USER}" ]]; then
  ANSIBLE_BECOME=" -become --become-user=${BECOME_USER} "
fi

ansible ${ANSIBLE_PATTERN} ${ANSIBLE_INVENTORY} -u ${ANSIBLE_SSH_USER} ${ANSIBLE_BECOME} -m ${ANSIBLE_MODULE_NAME} \
-a ${ANSIBLE_MODULE_ARGS}
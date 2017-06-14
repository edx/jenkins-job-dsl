#!/usr/bin/env bash

set -ex

cd $WORKSPACE/configuration
pip install -r requirements.txt
pip install awscli

env
export EC2_CACHE_PATH="ec2-cache"
. util/jenkins/assume-role.sh
set +x
# Assume the role that will allow running ec2.py for getting a dynamic inventory
assume-role ${ROLE_ARN}
set -x

cd $WORKSPACE/configuration/playbooks

# Pattern must be supplied explicitly as we take a conservative
# approach given that ec2.py will provide a dynamic inventory
if [[ -n "${PATTERN}" ]]; then
  ANSIBLE_PATTERN="${PATTERN}"
else
  ANSIBLE_PATTERN="__NONE__"
fi

if [[ -n "${INVENTORY}" ]]; then
  ANSIBLE_INVENTORY="-i ${INVENTORY} "
else
  ANSIBLE_INVENTORY="-i ./ec2.py"
fi

if [[ -n ${CUSTOM_INVENTORY} ]]; then
  HOSTS=$($CUSTOM_INVENTORY)
  if [[ -n ${HOSTS} ]]; then
      ANSIBLE_INVENTORY="-i ${HOSTS}"
  else
      echo "No HOSTS found from CUSTOM_INVENTORY - refusing to run ansible"
      exit 1
  fi
fi

if [[ -n "${BECOME_USER}" ]]; then
  ANSIBLE_BECOME=" --become --become-user=${BECOME_USER} "
fi

ansible ${ANSIBLE_PATTERN} ${ANSIBLE_INVENTORY} -u ${ANSIBLE_SSH_USER} ${ANSIBLE_BECOME} -m ${ANSIBLE_MODULE_NAME} \
-a "${ANSIBLE_MODULE_ARGS}"

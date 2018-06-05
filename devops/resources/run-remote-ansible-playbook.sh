#!/usr/bin/env bash

set -ex

cd $WORKSPACE/configuration
pip install -r requirements.txt
pip install awscli

env

# docker on OS-X includes your Mac's home directory in the socket path
# that SSH/Ansible uses for the control socket, pushing you over
# the 108 character limit.
if [ -f /.dockerenv ]; then
  export ANSIBLE_SSH_ARGS='-o ControlMaster=auto -o ControlPersist=60s -o ControlPath="/tmp/%C" -o ServerAliveInterval=30'
fi

cd $WORKSPACE/configuration/playbooks/

if [ -n "${ANSIBLE_PLAYBOOK}" ]; then
    ANSIBLE_PLAYBOOK="${ANSIBLE_PLAYBOOK}"
    echo "Playbook $ANSIBLE_PLAYBOOK"
else
    echo "You must specify PLAYBOOK"
    exit
fi

if [ -n "${TAGS}" ]; then
  ANSIBLE_TAGS="--tags ${TAGS}"
else
  ANSIBLE_TAGS=""
fi

if [ -n "${ANSIBLE_INVENTORY}" ]; then
    ANSIBLE_INVENTORY="${ANSIBLE_INVENTORY}"
else
    echo "You must specify ANSIBLE_INVENTORY"
    exit
fi

ansible-playbook -vvvv -i ${ANSIBLE_INVENTORY}, ${ANSIBLE_PLAYBOOK} -u ${ANSIBLE_SSH_USER} ${ANSIBLE_EXTRA_VARS} ${ANSIBLE_TAGS}

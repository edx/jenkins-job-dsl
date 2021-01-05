#!/usr/bin/env bash

set +u
. /edx/var/jenkins/jobvenvs/virtualenv_tools.sh
# creates a venv with its location stored in variable "venvpath"
create_virtualenv --python=python3.8 --clear
. "$venvpath/bin/activate"
set -u

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

export ANSIBLE_HOST_KEY_CHECKING=False

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

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

cd $WORKSPACE/configuration/playbooks

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

if [[ -n ${SKIP_DEPLOYMENT_FILES} ]]; then
  ANSIBLE_INCLUDES=$( cat <<INCLUDES
-e@$WORKSPACE/configuration-internal/ansible/vars/${ENVIRONMENT}-${DEPLOYMENT}.yml
-e@$WORKSPACE/configuration-secure/ansible/vars/${ENVIRONMENT}-${DEPLOYMENT}.yml
INCLUDES
)
else
  ANSIBLE_INCLUDES=$( cat <<INCLUDES2
-e@$WORKSPACE/configuration-internal/ansible/vars/${DEPLOYMENT}.yml
-e@$WORKSPACE/configuration-internal/ansible/vars/${ENVIRONMENT}-${DEPLOYMENT}.yml
-e@$WORKSPACE/configuration-secure/ansible/vars/${DEPLOYMENT}.yml
-e@$WORKSPACE/configuration-secure/ansible/vars/${ENVIRONMENT}-${DEPLOYMENT}.yml
INCLUDES2
)
fi

ansible-playbook -vvvv -i 127.0.0.1, -c local ${ANSIBLE_PLAYBOOK} -U $(whoami) \
${ANSIBLE_INCLUDES} \
${ANSIBLE_EXTRA_VARS} \
${ANSIBLE_TAGS}

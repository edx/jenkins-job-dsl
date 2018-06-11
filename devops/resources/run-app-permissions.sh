#!/bin/bash -xe

cd configuration
pip install -r pre-requirements.txt
pip install -r requirements.txt

env
. util/jenkins/assume-role.sh

assume-role ${ROLE_ARN}
cd $WORKSPACE/configuration/playbooks

INVENTORY=$(./active_instances_in_asg.py --asg ${ENVIRONMENT}-${DEPLOYMENT}-worker)
if [[ -n ${INVENTORY} ]]; then
    ansible-playbook -i ${INVENTORY} manage_edxapp_users_and_groups.yml -e@$WORKSPACE/app-permissions/${ENVIRONMENT}-${DEPLOYMENT}-edxapp.yml --user ${USER}
else
    echo "Skipping ${ENVIRONMENT} ${DEPLOYMENT}, no worker cluster available, get it next time"
fi

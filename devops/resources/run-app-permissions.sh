#!/bin/bash -xe

set +u
. /edx/var/jenkins/jobvenvs/virtualenv_tools.sh
# creates a venv with its location stored in variable "venvpath"
create_virtualenv --python=python3.8 --clear
. "$venvpath/bin/activate"
set -u

cd configuration
pip install -r pre-requirements.txt
pip install -r requirements.txt

env
. util/jenkins/assume-role.sh

assume-role ${ROLE_ARN}
cd $WORKSPACE/configuration/playbooks

INVENTORY=$(./active_instances_in_asg.py --asg ${ENVIRONMENT}-${DEPLOYMENT}-worker)

if [[ -n ${INVENTORY} ]]; then

    # LMS groups.
    ansible-playbook \
        -i ${INVENTORY} \
        manage_edxapp_groups.yml \
        -e@$WORKSPACE/app-permissions/groups/lms.yml \
        -e "service_variant=lms" \
        --user ${USER} \
        --tags manage-${JOB_TYPE}

    # CMS groups.
    ansible-playbook \
        -i ${INVENTORY} \
        manage_edxapp_groups.yml \
        -e@$WORKSPACE/app-permissions/groups/cms.yml \
        -e "service_variant=cms" \
        --user ${USER} \
        --tags manage-${JOB_TYPE}

    # LMS+CMS users.
    ansible-playbook \
        -i ${INVENTORY} \
        manage_edxapp_users.yml \
        -e@$WORKSPACE/app-permissions/users/${ENVIRONMENT}-${DEPLOYMENT}.yml \
        --user ${USER} \
        --tags manage-${JOB_TYPE}

else
    echo "Skipping ${ENVIRONMENT} ${DEPLOYMENT}, no worker cluster available, get it next time"
fi

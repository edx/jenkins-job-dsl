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

if [[ "$JOB_TYPE" =~ ^groups-(.+)$ ]]; then
	service="${BASH_REMATCH[1]}"
	configfile=${WORKSPACE}/app-permissions/groups/${service}.yml
elif [[ "$JOB_TYPE" = 'active-users' ]] || [[ "$JOB_TYPE" = 'inactive-users' ]]; then
	configfile=${WORKSPACE}/app-permissions/users/${ENVIRONMENT}-${DEPLOYMENT}.yml
else
	echo "Bad job type: ${JOB_TYPE}."
	echo "Expected active-users, inactive-users, or groups-<service>."
	exit 1
fi

INVENTORY=$(./active_instances_in_asg.py --asg ${ENVIRONMENT}-${DEPLOYMENT}-worker)
if [[ -n ${INVENTORY} ]]; then
    ansible-playbook -i ${INVENTORY} manage_edxapp_users_and_groups.yml \
                     -e@${configfile} -e "group_environment=${ENVIRONMENT}-${DEPLOYMENT}" \
                     --user ${USER} --tags manage-${JOB_TYPE}
else
    echo "Skipping ${ENVIRONMENT} ${DEPLOYMENT}, no worker cluster available, get it next time"
fi

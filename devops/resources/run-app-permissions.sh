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

# Function to set ASG and path variables
function setASGAndPath () {
	service_name="$1"
	if [[ "${service_name}" == "lms" ]] || [[ "${service_name}" == "cms" ]] || [[ "${service_name}" == "edxapp" ]]; then
		INVENTORY=$(./active_instances_in_asg.py --asg ${ENVIRONMENT}-${DEPLOYMENT}-worker)
		SERVICE_ENV_PATH="/edx/app/edxapp/edxapp_env"
		SERVICE_PYTHON_PATH="/edx/app/edxapp/venvs/edxapp/bin/python"
		SERVICE_MANAGE_PATH="/edx/bin/manage.edxapp"
	else
		INVENTORY=$(./active_instances_in_asg.py --asg ${ENVIRONMENT}-${DEPLOYMENT}-${service_name})
		SERVICE_ENV_PATH="/edx/app/${service_name}/${service_name}_env"
		SERVICE_PYTHON_PATH="/edx/app/${service_name}/venvs/${service_name}/bin/python"
		SERVICE_MANAGE_PATH="/edx/bin/manage.${service_name}"
	fi
}

if [[ "$JOB_TYPE" =~ ^groups-(.+)$ ]]; then
	service="${BASH_REMATCH[1]}"
	configfile=${WORKSPACE}/app-permissions/groups/${service}.yml
	setASGAndPath ${service}
	if [[ "${service}" == "lms" ]] || [[ "${service}" == "cms" ]]; then
		ANSIBLE_TAG="manage-$JOB_TYPE"
	else
		ANSIBLE_TAG="manage-groups-ida"
	fi
elif [[ "$JOB_TYPE" =~ ^(.+)-users-(.+)$ ]]; then
	job_type_prefix="${BASH_REMATCH[1]}"
	service="${BASH_REMATCH[2]}"
	configfile=${WORKSPACE}/app-permissions/users/${service}/${ENVIRONMENT}-${DEPLOYMENT}.yml
	setASGAndPath ${service}
	if [[ "${service}" == "edxapp" ]]; then
		ANSIBLE_TAG="manage-$JOB_TYPE"
	else
		ANSIBLE_TAG="manage-${job_type_prefix}-users-ida"
	fi
else
	echo "Bad job type: ${JOB_TYPE}."
	echo "Expected active-users-edxapp, inactive-users-edxapp, inactive-users-<service> or groups-<service>."
	exit 1
fi

if [[ -n ${INVENTORY} ]]; then
    ansible-playbook -i ${INVENTORY} manage_edxapp_users_and_groups.yml \
										 -e "env_path=${SERVICE_ENV_PATH}" -e "python_path=${SERVICE_PYTHON_PATH}" \
										 -e "manage_path=${SERVICE_MANAGE_PATH}" -e "service=${service}" \
                     -e@${configfile} -e "group_environment=${ENVIRONMENT}-${DEPLOYMENT}" \
                     --user ${USER} --tags ${ANSIBLE_TAG}
else
    echo "Skipping ${ENVIRONMENT} ${DEPLOYMENT}, no worker cluster available, get it next time"
fi

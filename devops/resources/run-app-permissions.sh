#!/bin/bash -xe

set +u
. /edx/var/jenkins/jobvenvs/virtualenv_tools.sh
# creates a venv with its location stored in variable "venvpath"
create_virtualenv --python=python3.8 --clear
. "$venvpath/bin/activate"
set -u

cd $WORKSPACE/configuration
pip install -r pre-requirements.txt
pip install -r requirements.txt
pip install -r util/jenkins/requirements.txt

env
. util/jenkins/assume-role.sh

#assume-role ${ROLE_ARN}
cd $WORKSPACE/configuration/playbooks

# Function to set ASG and path variables
function setASGAndPath () {
        unassume-role
        assume-role ${ROLE_ARN}
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
	configfile_relative_path=users/${service}/${ENVIRONMENT}-${DEPLOYMENT}.yml
	configfile=${WORKSPACE}/app-permissions/${configfile_relative_path}
	setASGAndPath ${service}
	if [[ "${service}" == "edxapp" ]]; then
		ANSIBLE_TAG="manage-$JOB_TYPE"
	else
		ANSIBLE_TAG="manage-${job_type_prefix}-users-ida"
	fi

	if [[ "${job_type_prefix}" == "recent" ]]; then
		current_configfile="${configfile}"
		configfile="${WORKSPACE}/recent-${ENVIRONMENT}-${DEPLOYMENT}.yml"
		pushd ${WORKSPACE}/app-permissions
		${WORKSPACE}/app-permissions/generate_recent_users.sh ${configfile_relative_path} > ${configfile}
		popd
	fi

else
	echo "Bad job type: ${JOB_TYPE}."
	echo "Expected active-users-edxapp, inactive-users-edxapp, recent-users-<service>, inactive-users-<service> or groups-<service>."
	exit 1
fi

pushd ${WORKSPACE}/app-permissions
${WORKSPACE}/app-permissions/generate_split_users.sh ${configfile}
popd

for SPLIT_FILE in ${WORKSPACE}/app-permissions/split-*.yml; do
    setASGAndPath ${service}

    if [[ -n ${INVENTORY} ]]; then
        for RETRIES in $(seq 3 -1 0); do
            set +e
            ansible-playbook -i ${INVENTORY} manage_edxapp_users_and_groups.yml \
                             -e "env_path=${SERVICE_ENV_PATH}" -e "python_path=${SERVICE_PYTHON_PATH}" \
                             -e "manage_path=${SERVICE_MANAGE_PATH}" -e "service=${service}" \
                             -e@${SPLIT_FILE} -e "group_environment=${ENVIRONMENT}-${DEPLOYMENT}" \
                             --user ${USER} --tags ${ANSIBLE_TAG}
            EXIT_CODE=$?
            if [ "${EXIT_CODE}" == 0 ]; then
                break
            elif [ "${RETRIES}" != 0 ]; then
                echo "EXIT CODE: ${EXIT_CODE}"
                echo "Retrying, ${RETRIES} remaining"
                set -e
                setASGAndPath ${service}
            else
                exit ${EXIT_CODE}
            fi
            set -e
        done
    else
        echo "Skipping ${ENVIRONMENT} ${DEPLOYMENT}, no worker cluster available, get it next time"
    fi

done

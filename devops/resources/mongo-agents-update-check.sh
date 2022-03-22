#!/bin/bash

set +u
. /edx/var/jenkins/jobvenvs/virtualenv_tools.sh
# creates a venv with its location stored in variable "venvpath"
create_virtualenv --python=python3.8 --clear
. "$venvpath/bin/activate"
set -u

set -x

cd $WORKSPACE/configuration

base_url=$(grep 'base_url' playbooks/roles/mongo_mms/defaults/main.yml | cut -d':' -f2- | tr -d '"')

curl -OL $base_url/automation/mongodb-mms-automation-agent-manager_latest_amd64.ubuntu1604.deb

available_mongo_automation_agent_version=$(dpkg-deb -f mongodb-mms-automation-agent-manager_latest_amd64.ubuntu1604.deb Version)

AVAILABLE_MONGO_AGENTS="$available_mongo_automation_agent_version"

installed_mongo_automation_agent=$(grep 'version' playbooks/roles/mongo_mms/defaults/main.yml | awk -F ":" 'NR == 2 {print $2}' | tr -d '" ')
INSTALLED_MONGO_AGENTS="$installed_mongo_automation_agent"

if [[ "$AVAILABLE_MONGO_AGENTS" != "$INSTALLED_MONGO_AGENTS" ]];then
    echo "Mongo Automation Agent Update is Available, You need to upgrade to $AVAILABLE_MONGO_AGENTS version by replacing it in https://github.com/openedx/configuration/blob/master/playbooks/roles/mongo_mms/defaults/main.yml#L11"
    exit 1
fi

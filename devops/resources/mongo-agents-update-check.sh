#!/bin/bash

set -x

cd $WORKSPACE/configuration

base_url=$(grep 'base_url' playbooks/roles/mongo_mms/defaults/main.yml | cut -d':' -f2- | tr -d '"')

curl -OL $base_url/monitoring/mongodb-mms-monitoring-agent_latest_amd64.ubuntu1604.deb
curl -OL $base_url/backup/mongodb-mms-backup-agent_latest_amd64.ubuntu1604.deb

available_monitoring_agent_version=$(dpkg-deb -f mongodb-mms-monitoring-agent_latest_amd64.ubuntu1604.deb Version)
available_backup_agent_version=$(dpkg-deb -f mongodb-mms-backup-agent_latest_amd64.ubuntu1604.deb Version)
AVAILABLE_MONGO_AGENTS="$available_monitoring_agent_version $available_backup_agent_version"

installed_monitoring_agent=$(grep 'version' playbooks/roles/mongo_mms/defaults/main.yml | awk -F ":" 'NR == 2 {print $2}' | tr -d '" ')
installed_backup_agent=$(grep 'version' playbooks/roles/mongo_mms/defaults/main.yml | awk -F ":" 'NR == 3 {print $2}' | tr -d '" ')
INSTALLED_MONGO_AGENTS="$installed_monitoring_agent $installed_backup_agent"

for agent in $AVAILABLE_MONGO_AGENTS;do
    echo ${INSTALLED_MONGO_AGENTS} | grep --quiet  "${agent}"
    if [[ ! $? -eq 0 ]];then
      echo "Mongo Agent Update is Available: ${agent}"
      exit 1
    fi
done

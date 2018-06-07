#!/bin/bash

set -exo pipefail

env

set -x

cd $WORKSPACE/configuration

base_url=$(grep 'base_url' playbooks/roles/mongo_mms/defaults/main.yml | cut -d':' -f2- | tr -d '"')

curl -OL $base_url/monitoring/mongodb-mms-monitoring-agent_latest_amd64.ubuntu1604.deb
curl -OL $base_url/backup/mongodb-mms-backup-agent_latest_amd64.ubuntu1604.deb

current_monitoring_agent=$(dpkg-deb -f mongodb-mms-monitoring-agent_latest_amd64.ubuntu1604.deb Version)
current_backup_agent=$(dpkg-deb -f mongodb-mms-backup-agent_latest_amd64.ubuntu1604.deb Version)
CUR_MONGO_AGENTS="$current_monitoring_agent $current_backup_agent"

installed_monitoring_agent=$(grep 'version' playbooks/roles/mongo_mms/defaults/main.yml | awk -F ":" 'NR == 2 {print $2}' | tr -d '" ')
installed_backup_agent=$(grep 'version' playbooks/roles/mongo_mms/defaults/main.yml | awk -F ":" 'NR == 3 {print $2}' | tr -d '" ')
INS_MONGO_AGENTS="$installed_monitoring_agent $installed_backup_agent"

for agent in $CUR_MONGO_AGENTS;do
    echo ${INS_MONGO_AGENTS} | grep --quiet  "${agent}"
    if [[ ! $? -eq 0 ]];then
      echo "Mongo Agent Update is Available: ${agent}"
      exit 1
    fi
done

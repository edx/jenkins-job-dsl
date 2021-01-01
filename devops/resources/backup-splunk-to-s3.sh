#!/bin/bash

set +u
. /edx/var/jenkins/jobvenvs/virtualenv_tools.sh
# creates a venv with its location stored in variable "venvpath"
create_virtualenv --python=python3.8 --clear
. "$venvpath/bin/activate"
set -u

cd $WORKSPACE/configuration
pip install -r requirements.txt

cd $WORKSPACE/configuration/playbooks/edx-east

ansible-playbook -u splunkbackup -i $host_ip,  splunk_config_backup.yml -e "splunk_host_id=$splunk_host \
      splunk_s3_backups_bucket=$s3_bucket splunk_backup_dir=$splunk_backup_dir"

#!/bin/bash
set -exuo pipefail

set +u
. /edx/var/jenkins/jobvenvs/virtualenv_tools.sh
# creates a venv with its location stored in variable "venvpath"
create_virtualenv --python=python3.8 --clear
. "$venvpath/bin/activate"
set -u

cd $WORKSPACE/configuration
pip install -r requirements.txt

. util/jenkins/assume-role.sh

assume-role ${ROLE_ARN}

cd playbooks

COMMON_AWS_SYNC_BUCKET="edx-${ENVIRONMENT}-${DEPLOYMENT}"
 
# Note that the TARGET far effectively is a limit as it is applied
# to the host match.  --limit returns a non-zero exit code, so it 
# has been abandoned.

ansible-playbook -u ubuntu -i lifecycle_inventory.py retire_host.yml -vvv -e COMMON_AWS_SYNC_BUCKET="${COMMON_AWS_SYNC_BUCKET}" -e COMMON_OBJECT_STORE_LOG_SYNC_BUCKET="${COMMON_AWS_SYNC_BUCKET}" -e COMMON_LOG_DIR="/edx/var/log" -e TARGET="${ENVIRONMENT}_${DEPLOYMENT}_Terminating_Wait"

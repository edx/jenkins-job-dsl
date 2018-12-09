#!/bin/bash
set -exuo pipefail

cd $WORKSPACE/configuration
pip install -r requirements.txt

. util/jenkins/assume-role.sh

assume-role ${ROLE_ARN}

cd playbooks/edx-east

COMMON_AWS_SYNC_BUCKET="edx-${ENVIRONMENT}-${DEPLOYMENT}"
 
# Note that the TARGET far effectively is a limit as it is applied
# to the host match.  --limit returns a non-zero exit code, so it 
# has been abandoned.

ansible-playbook -u ubuntu -i lifecycle_inventory.py retire_host.yml -vvv -e COMMON_AWS_SYNC_BUCKET="${COMMON_AWS_SYNC_BUCKET}" -e COMMON_OBJECT_STORE_LOG_SYNC_BUCKET="${COMMON_AWS_SYNC_BUCKET}" -e COMMON_LOG_DIR="/edx/var/log" -e TARGET="${ENVIRONMENT}_${DEPLOYMENT}_Terminating_Wait"

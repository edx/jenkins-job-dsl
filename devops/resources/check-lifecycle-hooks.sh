#!/bin/bash
set -euox pipefail

cd $WORKSPACE/sysadmin
pip install -r requirements.txt

if [ -z "${DEPLOYMENT}" ] || [ -z "${ASG}" ] || [ -z "${REGION}" ]; then
   exit 1
else

python aws-management/check-lifecycle-hooks.py --asg ${ASG} --hook GetTrackingLogs --profile ${DEPLOYMENT}
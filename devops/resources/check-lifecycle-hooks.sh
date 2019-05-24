#!/bin/bash
set -euox pipefail

cd $WORKSPACE/configuration
. util/jenkins/assume-role.sh

cd $WORKSPACE/sysadmin
pip install -r requirements.txt
export AWS_DEFAULT_REGION=us-east-1

: ${ASG?"Need to set ASG"}

assume-role ${ROLE_ARN}

python aws-management/check-lifecycle-hooks.py --asg ${ASG} --hook GetTrackingLogs

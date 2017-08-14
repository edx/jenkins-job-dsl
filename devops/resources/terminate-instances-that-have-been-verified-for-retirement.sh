#!/bin/bash
set -exuo pipefail

HOME=/edx/var/jenkins

env
set -x
cd $WORKSPACE/configuration
pip install -r requirements.txt

. util/jenkins/assume-role.sh

assume-role ${ROLE_ARN}

cd util/vpc-tools

python asg_lifcycle_watcher.py -r ${AWS_REGION} -q ${ENVIRONMENT}-${DEPLOYMENT}_autoscaling-lifecycle  --hook ${ENVIRONMENT}-${DEPLOYMENT}-GetTrackingLogs

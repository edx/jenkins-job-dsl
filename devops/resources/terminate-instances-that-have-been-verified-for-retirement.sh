#!/bin/bash
set -exuo pipefail

set +u
. /edx/var/jenkins/jobvenvs/virtualenv_tools.sh
# creates a venv with its location stored in variable "venvpath"
create_virtualenv --python=python3.8 --clear
. "$venvpath/bin/activate"
set -u

HOME=/edx/var/jenkins

env
set -x
cd $WORKSPACE/configuration
pip install -r requirements.txt

. util/jenkins/assume-role.sh

assume-role ${ROLE_ARN}

cd util/vpc-tools

python asg_lifcycle_watcher.py -r ${AWS_REGION} -q ${ENVIRONMENT}-${DEPLOYMENT}_autoscaling-lifecycle  --hook ${ENVIRONMENT}-${DEPLOYMENT}-GetTrackingLogs

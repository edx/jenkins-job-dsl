#!/bin/bash
set -euox pipefail

set +u
. /edx/var/jenkins/jobvenvs/virtualenv_tools.sh
# creates a venv with its location stored in variable "venvpath"
create_virtualenv --python=python3.8 --clear
. "$venvpath/bin/activate"
set -u

cd $WORKSPACE/configuration
. util/jenkins/assume-role.sh

cd $WORKSPACE/sysadmin
pip install -r requirements/base.txt
export AWS_DEFAULT_REGION=us-east-1

: ${ASG?"Need to set ASG"}

assume-role ${ROLE_ARN}

python aws-management/check-lifecycle-hooks.py --asg ${ASG} --hook GetTrackingLogs

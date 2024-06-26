#!/bin/bash
#we don't use u in the pipefail because Jenkins doesn't set variables and we want to check for that
set -exo pipefail

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

cd $WORKSPACE/monitoring-scripts
pip install -r requirements/base.txt
cd ssl_expiration_check

# Set RDSIGNORE if not set in job, need because we're setting -u
# Otherwise we get an error "RDSIGNORE: unbound variable"
if [[ ! -v RDSIGNORE ]]; then
    RDSIGNORE=""
fi

if [[ -n "${FROM_ADDRESS}" && "${TO_ADDRESS}" ]]; then
	python ssl-expiration-check.py --region $REGION -d $DAYS  -r $TO_ADDRESS -f $FROM_ADDRESS -i $RDSIGNORE
else
	python ssl-expiration-check.py --region $REGION -d $DAYS -i $RDSIGNORE
fi

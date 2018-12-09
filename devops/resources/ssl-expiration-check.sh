#!/bin/bash
#we don't use u in the pipefail because Jenkins doesn't set variables and we want to check for that
set -exo pipefail

HOME=/edx/var/jenkins

env
set -x

cd $WORKSPACE/configuration
pip install -r requirements.txt
. util/jenkins/assume-role.sh

assume-role ${ROLE_ARN}

cd $WORKSPACE/sysadmin
pip install -r requirements.txt
cd jenkins

if [[ -n "${FROM_ADDRESS}" && "${TO_ADDRESS}" ]]; then
	python ssl-expiration-check.py --region $REGION -d $DAYS  -r $TO_ADDRESS -f $FROM_ADDRESS
else
	python ssl-expiration-check.py --region $REGION -d $DAYS
fi

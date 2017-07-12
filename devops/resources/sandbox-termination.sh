#!/bin/bash
set -exuo pipefail

HOME=/edx/var/jenkins

env
set -x

cd $WORKSPACE/configuration
pip install -r requirements.txt
. util/jenkins/assume-role.sh

assume-role ${ROLE_ARN}
NOTIFICATION_CONFIG=$"${WORKSPACE}/configuration-internal/tools-edx-jenkins/sandbox-termination-notification-config.yml"

cd $WORKSPACE/sysadmin
pip install -r requirements.txt
cd jenkins

extra_args=""
if [ "$NOOP" = true ]; then
  extra_args="-n"
fi

python terminate-sandbox.py $extra_args -z $ROUTE53_ZONE -r $AWS_REGION -f $NOTIFICATION_CONFIG

curl https://nosnch.in/c6dcca38ad


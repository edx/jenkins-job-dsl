#!/bin/bash
set -exuo pipefail

HOME=/edx/var/jenkins

env
set -x

cd $WORKSPACE/configuration
pip install -r requirements.txt
. util/jenkins/assume-role.sh

assume-role ${ROLE_ARN}

cd $WORKSPACE/jenkins-job-dsl-internal
cd util
pip install -r requirements.txt

extra_args=""
if [ "$NOOP" = true ]; then
  extra_args="-n"
fi

python terminate-sandbox.py $extra_args -z $ROUTE53_ZONE -r $AWS_REGION --hipchat_room $HIPCHAT_ROOM --hipchat_api_key $HIPCHAT_API_KEY --edx_git_bot_token $EDX_GIT_BOT_TOKEN

curl $SNITCH


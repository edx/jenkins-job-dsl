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

cd $WORKSPACE/jenkins-job-dsl-internal
cd util/sandbox_terminate
pip install -r requirements.txt

extra_args=""
if [ "$NOOP" = true ]; then
  extra_args="-n"
fi

python terminate-sandbox.py $extra_args -z $ROUTE53_ZONE -r $AWS_REGION --edx_git_bot_token $EDX_GIT_BOT_TOKEN


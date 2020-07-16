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
cd util/janitor
pip install -r requirements.txt


if [ "$NOOP" = true ]; then
   python janitor.py --noop --region $AWS_REGION --cleaner $AWS_CLEANER --log-bucket $S3_LOG_BUCKET
else
  python janitor.py --region $AWS_REGION --cleaner $AWS_CLEANER --log-bucket $S3_LOG_BUCKET
fi

curl -X GET 'https://api.opsgenie.com/v2/heartbeats/'${JOB_NAME##*/}'/ping' -H 'Authorization: GenieKey '${GENIE_KEY}

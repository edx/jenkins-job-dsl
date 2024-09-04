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
cd util/janitor
pip install -r requirements.txt

deny_prospectus=0

if [ -v DENY_LIST ]; then
  for play in "${DENY_LIST}"; do
    if [[ "$play" == "prospectus" ]]; then
      deny_prospectus=1
      break
    fi
  done
fi

if [ "$NOOP" = true ]; then
  python janitor.py --noop --region $AWS_REGION --cleaner $AWS_CLEANER --log-bucket $S3_LOG_BUCKET --deny-list $DENY_LIST
elif [ "$deny_prospectus" == 1 ]; then
  python janitor.py --region $AWS_REGION --cleaner $AWS_CLEANER --log-bucket $S3_LOG_BUCKET --deny-list $DENY_LIST
else
  python prospectus-janitor.py --region $AWS_REGION --cleaner $AWS_CLEANER --log-bucket $S3_LOG_BUCKET
fi

curl -X GET 'https://api.opsgenie.com/v2/heartbeats/'${JOB_NAME##*/}'/ping' -H 'Authorization: GenieKey '${GENIE_KEY}
curl -X POST "https://api.datadoghq.com/api/v1/series?api_key=${DD_KEY}" \
-H "Content-Type: application/json" \
-d '{
      "series" : [{
          "metric": '${JOB_NAME##*/}".heartbeat"',
          "points": [['"$(date +%s)"', 1]],
          "type": "gauge",
          "tags": ["deployment:'${DEPLOYMENT}'"]
      }]
  }'

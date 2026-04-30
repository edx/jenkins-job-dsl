#!/bin/bash

set +u
. /edx/var/jenkins/jobvenvs/virtualenv_tools.sh
# creates a venv with its location stored in variable "venvpath"
create_virtualenv --python=python3.8 --clear
. "$venvpath/bin/activate"
set -u

cd $WORKSPACE/configuration/util/jenkins/retired_user_cert_remover

pip install -r requirements.txt
. ../assume-role.sh

# Assume role for different envs
set +x
assume-role ${ROLE_ARN}

# Fetch LMS client credentials and base URL from the same Secrets Manager secret
# used by the retirement pipeline, so no separate DB credentials are needed.
SECRET_JSON=$(aws secretsmanager get-secret-value \
    --secret-id "user-retirement-secure/${ENVIRONMENT}-${DEPLOYMENT}" \
    --region "${AWS_DEFAULT_REGION}" \
    --output json | jq -r '.SecretString')

LMS_HOST=$(echo "${SECRET_JSON}" | jq -r '.base_urls.lms')
LMS_CLIENT_ID=$(echo "${SECRET_JSON}" | jq -r '.client_id')
LMS_CLIENT_SECRET=$(echo "${SECRET_JSON}" | jq -r '.client_secret')
unset SECRET_JSON

# Fail fast if any required value is missing or null in the secret.
if [ -z "${LMS_HOST}" ] || [ "${LMS_HOST}" = "null" ] || \
   [ -z "${LMS_CLIENT_ID}" ] || [ "${LMS_CLIENT_ID}" = "null" ] || \
   [ -z "${LMS_CLIENT_SECRET}" ] || [ "${LMS_CLIENT_SECRET}" = "null" ]; then
    echo "Error: required LMS secret values are missing or invalid in user-retirement-secure/${ENVIRONMENT}-${DEPLOYMENT}" >&2
    exit 1
fi
set -x

export LMS_CLIENT_ID
export LMS_CLIENT_SECRET

python ./retired_user_cert_remover.py \
    --lms-host="${LMS_HOST}" \
    --dry-run

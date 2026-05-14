#!/bin/bash

set -ex

VENV="venv-${BUILD_NUMBER}"
virtualenv --python=python3.8 --clear "${VENV}"
source "${VENV}/bin/activate"

cd $WORKSPACE/configuration/util/jenkins/retired_user_cert_remover

pip install -r requirements.txt

set +x

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

DRY_RUN="${DRY_RUN:-true}"
if [ "${DRY_RUN}" != "true" ] && [ "${DRY_RUN}" != "false" ]; then
    echo "Error: DRY_RUN must be 'true' or 'false', got '${DRY_RUN}'" >&2
    exit 1
fi

BATCH_SIZE="${BATCH_SIZE:-0}"
if ! echo "${BATCH_SIZE}" | grep -qE '^[0-9]+$'; then
    echo "Error: BATCH_SIZE must be a non-negative integer, got '${BATCH_SIZE}'" >&2
    exit 1
fi

python ./retired_user_cert_remover.py \
    --lms-host="${LMS_HOST}" \
    --dry-run="${DRY_RUN}" \
    --batch-size="${BATCH_SIZE}"

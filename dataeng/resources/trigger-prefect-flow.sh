#!/usr/bin/env bash
set -ex

# Creating python3.8 virtual env
PYTHON_VENV="python_venv"
virtualenv --python=python3.8 --clear "${PYTHON_VENV}"
source "${PYTHON_VENV}/bin/activate"

# Install prefect python pkg
cd $WORKSPACE/prefect-flows
pip install -r requirements.txt

# Do not print commands in this function since they may contain secrets.
set +x

# Retrieve a vault token corresponding to the jenkins AppRole.  The token is then stored in the VAULT_TOKEN variable
# which is implicitly used by subsequent vault commands within this script.
# Instructions followed: https://learn.hashicorp.com/tutorials/vault/approle#step-4-login-with-roleid-secretid
export VAULT_TOKEN=$(vault write -field=token auth/approle/login \
      role_id=${ANALYTICS_VAULT_ROLE_ID} \
      secret_id=${ANALYTICS_VAULT_SECRET_ID}
  )

PREFECT_CLOUD_AGENT_TOKEN=$(
  vault kv get \
    -version=${PREFECT_VAULT_KV_VERSION} \
    -field=PREFECT_CLOUD_AGENT_TOKEN \
    ${PREFECT_VAULT_KV_PATH} \
)

# Get Authenticated with Prefect Cloud
prefect auth login --key $PREFECT_CLOUD_AGENT_TOKEN

set -x

# Trigger prefect flow
prefect run --id $ENTERPRISE_PREFECT_FLOW_ID

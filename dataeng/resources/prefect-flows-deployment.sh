#!/usr/bin/env bash
set -ex

# Creating python3.8 virtual env
PYTHON_VENV="python_venv"
virtualenv --python=python3.8 --clear "${PYTHON_VENV}"
source "${PYTHON_VENV}/bin/activate"

# Removing prefix 'prefect-flows-deployment-'
# $FLOW_NAME will contain the name of flow going to be deployed
FLOW_NAME=$(echo $JOB_NAME | cut -c 26-)

# Install prefect-flow requirements, as it contains edx-prefectutils in itself so no need to install it separately
cd $WORKSPACE/prefect-flows
pip install -r requirements.txt

# Get ECR authetication
aws ecr get-login-password --region us-east-1 | docker login --username AWS --password-stdin $ECR_LOGIN

# The following statement will help us to determine if repository already exists otherwise it will create a new repository with the name of flow
aws ecr describe-repositories --repository-names $FLOW_NAME --region us-east-1 || aws ecr create-repository --repository-name $FLOW_NAME --region us-east-1

# Preparing to Autheticate with Prefect Cloud by getting token from Vault
vault write -field=token auth/approle/login \
  role_id=${ANALYTICS_VAULT_ROLE_ID} \
  secret_id=${ANALYTICS_VAULT_SECRET_ID} \
| vault login -no-print token=-

PREFECT_CLOUD_AGENT_TOKEN=$(
  vault kv get \
    -version=${PREFECT_VAULT_KV_VERSION} \
    -field=PREFECT_CLOUD_AGENT_TOKEN \
    ${PREFECT_VAULT_KV_PATH} \
)

# Get Authenticated with Prefect Cloud
prefect auth login --key $PREFECT_CLOUD_AGENT_TOKEN

# Deploy the flow. $FLOW_NAME will contain the name of flow to be deployed
make -C flows $FLOW_NAME

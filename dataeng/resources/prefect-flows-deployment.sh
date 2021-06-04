#!/usr/bin/env bash
set -ex

# Creating python3.8 virtual env
PLATFORM_VENV="platform_venv"
virtualenv --python=python3.8 --clear "${PLATFORM_VENV}"
source "${PLATFORM_VENV}/bin/activate"

# Removing prefix 'prefect-flows-deployment-'
# $FLOW will contain the name of flow
FLOW=$(echo $JOB_NAME | cut -c 26-)

cd $WORKSPACE/edx-prefectutils
pip install -r requirements/base.txt
pip install -r requirements/base.in

# Install prefect-flow requirements
cd $WORKSPACE/prefect-flows
pip install -r requirements.txt



# Get ECR authetication
aws ecr get-login-password --region us-east-1 | docker login --username AWS --password-stdin 216367352155.dkr.ecr.us-east-1.amazonaws.com

# The following statement will help us to determine if repository already exists otherwise it will create a new repository with the name of flow
aws ecr describe-repositories --repository-names $FLOW --region us-east-1 || aws ecr create-repository --repository-name $FLOW --region us-east-1

# Install prefect in jenkins python3.8 venv
#pip install prefect


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
prefect auth login -t $PREFECT_CLOUD_AGENT_TOKEN

# Deploy the flow. $FLOW will contain the name of flow to be deployed
make -C flows $FLOW
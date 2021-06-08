#!/usr/bin/env bash
set -ex

# Creating python3.8 virtual env
PYTHON_VENV="python_venv"
virtualenv --python=python3.8 --clear "${PYTHON_VENV}"
source "${PYTHON_VENV}/bin/activate"

# Removing prefix 'prefect-flows-deployment-'
# $FLOW will contain the name of flow
FLOW=$(echo $JOB_NAME | cut -c 26-)

#cd $WORKSPACE/edx-prefectutils
#pip install -r requirements/base.txt
#pip install -r requirements/base.in

# Install prefect-flow requirements
cd $WORKSPACE/prefect-flows
pip install -r requirements.txt



# Get ECR authetication
aws ecr get-login-password --region us-east-1 | docker login --username AWS --password-stdin $ECR_LOGIN

# Check if this is first ever run of the job. If yes, then create ECR repository otherwise do not
####### uncomment the following code after deploying all existing flows first time #######
# if [ "$BUILD_NUMBER" -eq 1 ]
# then
#     echo "Creating repository as its a first build"
#     aws ecr create-repository --repository-name $FLOW --region us-east-1
# else
#     echo "Not creating repository as its not a first build"
# fi
####### uncomment the above code after deploying all existing flows first time #######

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
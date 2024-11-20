#!/usr/bin/env bash
set -ex

# Creating python3.9 virtual env
PYTHON_VENV="python_venv"
virtualenv --python=python3.9 --clear "${PYTHON_VENV}"
source "${PYTHON_VENV}/bin/activate"

# Removing prefix 'prefect-flows-deployment-'
# $FLOW_NAME will contain the name of flow going to be deployed
FLOW_NAME=$(echo $JOB_NAME | cut -c 26-)

# Install prefect-flow requirements, as it contains edx-prefectutils in itself so no need to install it separately
cd $WORKSPACE/prefect-flows
pip install -r requirements.txt

# prune unused images, if there are any to prune.
unused_images=$(docker images -f dangling=true -q)
if [ -n "$unused_images" ]; then
    docker rmi -f $unused_images
fi

# Get ECR authetication
aws ecr get-login-password --region us-east-1 | docker login --username AWS --password-stdin $ECR_LOGIN

# The following statement will help us to determine if repository already exists otherwise it will create a new repository with the name of flow
aws ecr describe-repositories --repository-names $FLOW_NAME --region us-east-1 || aws ecr create-repository --repository-name $FLOW_NAME --region us-east-1

# Do not print commands in this function since they may contain secrets.
set +x

# Fetch the secrets from AWS
PREFECT_CLOUD_AGENT_TOKEN=$(aws secretsmanager get-secret-value --secret-id analytics-secure/prefect-cd --region us-east-1 --query SecretString --output text | jq -r ".PREFECT_CLOUD_AGENT_TOKEN")

# Get Authenticated with Prefect Cloud
prefect auth login --key $PREFECT_CLOUD_AGENT_TOKEN

set -x
# Deploy the flow. $FLOW_NAME will contain the name of flow to be deployed
make -C flows $FLOW_NAME

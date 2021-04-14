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
pip install prefect

# Get Authenticated with Prefect Cloud
prefect auth login -t $PREFECT_API_TOKEN

# Deploy the flow. $FLOW will contain the name of flow to be deployed
make -C flows $FLOW
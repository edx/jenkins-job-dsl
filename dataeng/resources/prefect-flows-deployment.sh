#!/usr/bin/env bash
set -ex

PLATFORM_VENV="platform_venv"
virtualenv --python=python3.8 --clear "${PLATFORM_VENV}"
source "${PLATFORM_VENV}/bin/activate"

# Setup to run dbt commands
cd $WORKSPACE/prefect-flows

#aws ecr get-login-password --region us-east-1 | docker login --username AWS --password-stdin 216367352155.dkr.ecr.us-east-1.amazonaws.com
pip install prefect
prefect auth login -t $PREFECT_API_TOKEN

cd $WORKSPACE/prefect-flows
pip install -r requirements.txt
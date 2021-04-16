#!/usr/bin/env bash
set -ex


# Setup to run dbt commands
cd $WORKSPACE/prefect-flows

aws ecr get-login-password --region us-east-1 | docker login --username AWS --password-stdin 216367352155.dkr.ecr.us-east-1.amazonaws.com

prefect auth login -t $PREFECT_API_TOKEN

cd $WORKSPACE/prefect-flows
pip install -r requirements.txt
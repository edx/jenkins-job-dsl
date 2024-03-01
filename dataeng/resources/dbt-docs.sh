#!/usr/bin/env bash
set -ex

# Creating python 3.8 virtual environment to run dbt warehouse-transform job
PYTHON38_VENV="py38_venv"
virtualenv --python=python3.8 --clear "${PYTHON38_VENV}"
source "${PYTHON38_VENV}/bin/activate"

# Setup
cd $WORKSPACE/warehouse-transforms

# To install right version of dbt
pip install -r requirements.txt

cd $WORKSPACE/warehouse-transforms/projects/reporting

source secrets-manager.sh analytics-secure/warehouse-transforms/profiles DBT_PASSWORD

dbt clean --profiles-dir $WORKSPACE/profiles/ --profile $DBT_PROFILE --target $DBT_TARGET
dbt deps --profiles-dir $WORKSPACE/profiles/ --profile $DBT_PROFILE --target $DBT_TARGET

# Builds the doc files to projects/reporting/target
dbt docs generate --profiles-dir $WORKSPACE/profiles/ --profile $DBT_PROFILE --target $DBT_TARGET

# Upload the buld docs to S3
pip install awscli

aws s3 cp --recursive ${WORKSPACE}/warehouse-transforms/projects/reporting/target/ s3://edx-dbt-docs/

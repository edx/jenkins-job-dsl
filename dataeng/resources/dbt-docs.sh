#!/usr/bin/env bash
set -ex

# Creating python 3.11 virtual environment to run dbt warehouse-transform job
PYTHON311_VENV="py311_venv"
virtualenv --python=python3.11 --clear "${PYTHON311_VENV}"
source "${PYTHON311_VENV}/bin/activate"

# Setup
cd $WORKSPACE/warehouse-transforms

# To install right version of dbt
pip install -r requirements.txt

cd $WORKSPACE/warehouse-transforms/projects/reporting

# Source the secrets-manager.sh script to make the function available
source $WORKSPACE/secrets-manager.sh
# Fetch the secrets from AWS
set +x
get_secret_value analytics-secure/warehouse-transforms/profiles PRIVATE_KEY
set -x
export PRIVATE_KEY

dbt clean --profiles-dir $WORKSPACE/warehouse-transforms/profiles/ --profile $DBT_PROFILE --target $DBT_TARGET
dbt deps --profiles-dir $WORKSPACE/warehouse-transforms/profiles/ --profile $DBT_PROFILE --target $DBT_TARGET

# Builds the doc files to projects/reporting/target
dbt docs generate --profiles-dir $WORKSPACE/warehouse-transforms/profiles/ --profile $DBT_PROFILE --target $DBT_TARGET

# Upload the buld docs to S3
pip install awscli

aws s3 cp --recursive ${WORKSPACE}/warehouse-transforms/projects/reporting/target/ s3://edx-dbt-docs/

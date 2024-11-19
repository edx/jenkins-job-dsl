#!/usr/bin/env bash
set -ex

# Creating python 3.12 virtual environment to run dbt warehouse-transform job
PYTHON_VENV="py312_venv"
/opt/python3.12/bin/python3.12 -m virtualenv --python=python3.12 --clear "${PYTHON_VENV}"
source "${PYTHON_VENV}/bin/activate"

# Setup
cd $WORKSPACE/warehouse-transforms
# To install right version of dbt
pip install -r requirements.txt


cd $WORKSPACE/warehouse-transforms/projects/$DBT_PROJECT

# Choose the marts from which to transfer DBT models based on Jenkins job parameter.
if [ "$MODELS_TO_TRANSFER" = 'daily' ]
then
    MART_NAME=programs_reporting
elif [ "$MODELS_TO_TRANSFER" = 'enterprise' ]
then
    MART_NAME=enterprise
else
    echo "Unknown MODELS_TO_TRANSFER: '${MODELS_TO_TRANSFER}'"
    exit 1
fi

ARGS="{mart: ${MART_NAME} }"

source $WORKSPACE/secrets-manager.sh
# Fetch the secrets from AWS
set +x
get_secret_value analytics-secure/warehouse-transforms/profiles DBT_PASSWORD
set -x
export DBT_PASSWORD

dbt deps --profiles-dir $WORKSPACE/warehouse-transforms/profiles --profile $DBT_PROFILE --target $DBT_TARGET

# Call DBT to perform all transfers for this mart.
dbt run-operation perform_s3_transfers --args "${ARGS}" --profile $DBT_PROFILE --target $DBT_TARGET --profiles-dir $WORKSPACE/warehouse-transforms/profiles/

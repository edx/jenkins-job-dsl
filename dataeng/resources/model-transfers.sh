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

source secrets-manager.sh analytics-secure/warehouse-transforms/profiles DBT_PASSWORD

dbt deps --profiles-dir $WORKSPACE/profiles --profile $DBT_PROFILE --target $DBT_TARGET

# Call DBT to perform all transfers for this mart.
dbt run-operation perform_s3_transfers --args "${ARGS}" --profile $DBT_PROFILE --target $DBT_TARGET --profiles-dir $WORKSPACE/analytics-secure/warehouse-transforms/

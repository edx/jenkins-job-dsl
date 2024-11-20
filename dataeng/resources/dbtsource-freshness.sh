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

source $WORKSPACE/secrets-manager.sh
# Fetch the secrets from AWS
set +x
get_secret_value analytics-secure/warehouse-transforms/profiles DBT_PASSWORD
set -x
export DBT_PASSWORD

dbt clean --profiles-dir $WORKSPACE/warehouse-transforms/profiles/ --profile $DBT_PROFILE --target $DBT_TARGET
dbt deps --profiles-dir $WORKSPACE/warehouse-transforms/profiles/ --profile $DBT_PROFILE --target $DBT_TARGET

# For dbt v0.21.0 or above, dbt source snapshot-freshness has been renamed to dbt source freshness.
# Its node selection logic is now consistent with other tasks. In order to check freshness for a specific source,
# use --select flag and you must prefix it with source:   e.g. dbt source freshness --select source:snowplow
dbt source freshness --profiles-dir $WORKSPACE/warehouse-transforms/profiles/ --profile $DBT_PROFILE --target $DBT_TARGET

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

# For dbt v0.21.0 or above, dbt source snapshot-freshness has been renamed to dbt source freshness.
# Its node selection logic is now consistent with other tasks. In order to check freshness for a specific source,
# use --select flag and you must prefix it with source:   e.g. dbt source freshness --select source:snowplow
dbt source freshness --profiles-dir $WORKSPACE/profiles/ --profile $DBT_PROFILE --target $DBT_TARGET


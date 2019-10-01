#!/usr/bin/env bash
set -ex

# Setup
cd $WORKSPACE/warehouse-transforms
# To install right version of dbt
pip install -r tools/dbt_schema_builder/requirements.txt

cd $DBT_PROJECT
dbt clean

cd $WORKSPACE/warehouse-transforms

dbt run --models tag:$MODEL_TAG --project $DBT_PROJECT --profile $DBT_PROFILE --target $DBT_TARGET --profiles-dir $WORKSPACE/analytics-secure/warehouse-transforms/


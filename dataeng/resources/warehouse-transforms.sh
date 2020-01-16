#!/usr/bin/env bash
set -ex

# Setup
cd $WORKSPACE/warehouse-transforms
# To install right version of dbt
pip install -r tools/dbt_schema_builder/requirements.txt


cd $WORKSPACE/warehouse-transforms/warehouse_transforms_project

dbt clean
dbt deps
dbt seed --profile $DBT_PROFILE --target $DBT_TARGET --profiles-dir $WORKSPACE/analytics-secure/warehouse-transforms/
dbt run --models tag:$MODEL_TAG --profile $DBT_PROFILE --target $DBT_TARGET --profiles-dir $WORKSPACE/analytics-secure/warehouse-transforms/
dbt test --models tag:$MODEL_TAG --profile $DBT_PROFILE --target $DBT_TARGET --profiles-dir $WORKSPACE/analytics-secure/warehouse-transforms/

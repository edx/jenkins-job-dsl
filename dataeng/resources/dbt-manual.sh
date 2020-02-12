#!/usr/bin/env bash
set -ex

# Setup
cd $WORKSPACE/warehouse-transforms

# To install right version of dbt
pip install -r tools/dbt_schema_builder/requirements.txt

cd $WORKSPACE/warehouse-transforms/$DBT_PROJECT_PATH

dbt clean
dbt deps
dbt seed --profile $DBT_PROFILE --target $DBT_TARGET --profiles-dir $WORKSPACE/analytics-secure/warehouse-transforms/
dbt run $DBT_RUN_OPTIONS $DBT_RUN_EXCLUDE --profile $DBT_PROFILE --target $DBT_TARGET --profiles-dir $WORKSPACE/analytics-secure/warehouse-transforms/
dbt test $DBT_TEST_OPTIONS --profile $DBT_PROFILE --target $DBT_TARGET --profiles-dir $WORKSPACE/analytics-secure/warehouse-transforms/
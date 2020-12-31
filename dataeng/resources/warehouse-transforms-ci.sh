#!/usr/bin/env bash
set -ex
# This is work in progress script 
# Need to work on snowflake schema and correct profile

# Setup
cd $WORKSPACE/warehouse-transforms

# To install right version of dbt
pip install -r requirements.txt

# where this WORKSPACE is defined ? # Comment to be removed
cd $WORKSPACE/warehouse-transforms/projects/$DBT_PROJECT_PATH


dbt clean --profiles-dir $WORKSPACE/analytics-secure/warehouse-transforms/ --profile $DBT_PROFILE --target $DBT_TARGET
dbt deps --profiles-dir $WORKSPACE/analytics-secure/warehouse-transforms/ --profile $DBT_PROFILE --target $DBT_TARGET
dbt seed --profile $DBT_PROFILE --target $DBT_TARGET --profiles-dir $WORKSPACE/analytics-secure/warehouse-transforms/
dbt run $DBT_RUN_OPTIONS $DBT_RUN_EXCLUDE --profile $DBT_PROFILE --target $DBT_TARGET --profiles-dir $WORKSPACE/analytics-secure/warehouse-transforms/
dbt test $DBT_RUN_OPTIONS --profile $DBT_PROFILE --target $DBT_TARGET --profiles-dir $WORKSPACE/analytics-secure/warehouse-transforms/

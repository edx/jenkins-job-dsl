#!/usr/bin/env bash
set -ex
# This is work in progress script 
# Need to work on snowflake schema and correct profile

# Setup
cd $WORKSPACE/warehouse-transforms

# To install right version of dbt
pip install -r requirements.txt

cd $WORKSPACE/warehouse-transforms/projects/$DBT_PROJECT_PATH


dbt clean --profiles-dir $WORKSPACE/analytics-secure/warehouse-transforms/ --profile $DBT_PROFILE --target $DBT_TARGET
dbt deps --profiles-dir $WORKSPACE/analytics-secure/warehouse-transforms/ --profile $DBT_PROFILE --target $DBT_TARGET

# WIP: Running on only google analytics sessions models tag to speed up the test runs
dbt test --models tag:google_analytics_sessions --exclude 'source:*' --profile warehouse_transforms --target prod --profiles-dir $WORKSPACE/analytics-secure/warehouse-transforms/
#dbt test $DBT_RUN_OPTIONS --profile $DBT_PROFILE --target $DBT_TARGET --profiles-dir $WORKSPACE/analytics-secure/warehouse-transforms/


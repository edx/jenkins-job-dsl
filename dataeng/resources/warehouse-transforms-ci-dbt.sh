#!/usr/bin/env bash
set -ex

# This script runs the dbt deps/seed/run/test commands and serve warehouse-transforms-ci.sh script.
# DBT_RDBT_RUN_OPTIONS, DBT_TEST_OPTIONS etc comes from warehouse-transforms-ci.sh

cd $WORKSPACE/warehouse-transforms/projects/$DBT_PROJECT_PATH

dbt clean --profiles-dir $WORKSPACE/analytics-secure/warehouse-transforms/ --profile $DBT_PROFILE --target $DBT_TARGET
dbt deps --profiles-dir $WORKSPACE/analytics-secure/warehouse-transforms/ --profile $DBT_PROFILE --target $DBT_TARGET
dbt seed --profiles-dir $WORKSPACE/analytics-secure/warehouse-transforms/ --profile $DBT_PROFILE --target $DBT_TARGET

dbt run $DBT_RUN_OPTIONS $DBT_RUN_EXCLUDE --profiles-dir $WORKSPACE/analytics-secure/warehouse-transforms/ --profile $DBT_PROFILE --target $DBT_TARGET
dbt test $DBT_TEST_OPTIONS $DBT_TEST_EXCLUDE --profiles-dir $WORKSPACE/analytics-secure/warehouse-transforms/ --profile $DBT_PROFILE --target $DBT_TARGET
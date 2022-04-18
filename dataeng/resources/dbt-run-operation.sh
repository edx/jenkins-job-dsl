#!/usr/bin/env bash

# dbt-run-operation.sh
# a generic script to call dbt run-operation, calling the macro $OPERATION_NAME
# and any additional macro args specified in $RUN_ARGS

set -ex

cd $WORKSPACE/warehouse-transforms
pip install -r requirements.txt

cd $WORKSPACE/warehouse-transforms/projects/$DBT_PROJECT

dbt clean
dbt deps --profiles-dir $WORKSPACE/analytics-secure/warehouse-transforms/ --profile $DBT_PROFILE --target $DBT_TARGET

dbt run-operation $OPERATION_NAME --args "$RUN_ARGS" --profile $DBT_PROFILE --target $DBT_TARGET --profiles-dir $WORKSPACE/analytics-secure/warehouse-transforms/

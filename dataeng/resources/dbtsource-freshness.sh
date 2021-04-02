#!/usr/bin/env bash
set -ex

# Setup
cd $WORKSPACE/warehouse-transforms

# To install right version of dbt
pip install -r requirements.txt

cd $WORKSPACE/warehouse-transforms/projects/reporting

dbt clean --profiles-dir $WORKSPACE/analytics-secure/warehouse-transforms/ --profile $DBT_PROFILE --target $DBT_TARGET
dbt deps --profiles-dir $WORKSPACE/analytics-secure/warehouse-transforms/ --profile $DBT_PROFILE --target $DBT_TARGET

# Use the --select flag to snapshot freshness for specific sources, [--select [source_1, ...]]
dbt source snapshot-freshness --profiles-dir $WORKSPACE/analytics-secure/warehouse-transforms/ --profile $DBT_PROFILE --target $DBT_TARGET


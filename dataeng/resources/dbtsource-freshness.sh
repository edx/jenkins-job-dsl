#!/usr/bin/env bash
set -ex

# Setup
cd $WORKSPACE/warehouse-transforms

# To install right version of dbt
pip install -r requirements.txt

cd $WORKSPACE/warehouse-transforms/projects/reporting

dbt clean --profiles-dir $WORKSPACE/analytics-secure/warehouse-transforms/ --profile $DBT_PROFILE --target $DBT_TARGET
dbt deps --profiles-dir $WORKSPACE/analytics-secure/warehouse-transforms/ --profile $DBT_PROFILE --target $DBT_TARGET

# For dbt v0.21.0 or above, dbt source snapshot-freshness has been renamed to dbt source freshness.
# Its node selection logic is now consistent with other tasks. In order to check freshness for a specific source,
# use --select flag and you must prefix it with source:   e.g. dbt source freshness --select source:snowplow
dbt source freshness --profiles-dir $WORKSPACE/analytics-secure/warehouse-transforms/ --profile $DBT_PROFILE --target $DBT_TARGET


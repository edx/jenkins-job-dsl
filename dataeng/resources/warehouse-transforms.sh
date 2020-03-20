#!/usr/bin/env bash
set -ex

# Setup
cd $WORKSPACE/warehouse-transforms
# To install right version of dbt
pip install -r tools/dbt_schema_builder/requirements.txt


cd $WORKSPACE/warehouse-transforms/projects/reporting

dbt clean
dbt deps
dbt seed --profile $DBT_PROFILE --target $DBT_TARGET --profiles-dir $WORKSPACE/analytics-secure/warehouse-transforms/

# Run the source tests first, sadly not just the ones upstream from this tag
dbt test --models source:* --profile $DBT_PROFILE --target $DBT_TARGET --profiles-dir $WORKSPACE/analytics-secure/warehouse-transforms/
dbt run --models tag:$MODEL_TAG --profile $DBT_PROFILE --target $DBT_TARGET --profiles-dir $WORKSPACE/analytics-secure/warehouse-transforms/

# Run the tests for this tag, but exclude sources since we just tested them
dbt test --models tag:$MODEL_TAG --exclude source:* --profile $DBT_PROFILE --target $DBT_TARGET --profiles-dir $WORKSPACE/analytics-secure/warehouse-transforms/

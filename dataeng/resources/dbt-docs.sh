#!/usr/bin/env bash
set -ex

# Setup
cd $WORKSPACE/warehouse-transforms

# To install right version of dbt
pip install -r tools/dbt_schema_builder/requirements.txt

cd $WORKSPACE/warehouse-transforms/projects/reporting

dbt clean
dbt deps

# Builds the doc files to projects/reporting/target
dbt docs generate --profiles-dir $WORKSPACE/analytics-secure/warehouse-transforms/ --profile $DBT_PROFILE --target $DBT_TARGET

# Upload the buld docs to S3
pip install awscli

aws s3 cp --recursive ${WORKSPACE}/warehouse-transforms/projects/reporting/target/ s3://edx-dbt-docs/

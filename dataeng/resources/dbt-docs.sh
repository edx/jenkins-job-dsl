#!/usr/bin/env bash
set -ex

# Setup
cd $WORKSPACE/warehouse-transforms

# To install right version of dbt
pip install -r tools/dbt_schema_builder/requirements.txt

cd $WORKSPACE/warehouse-transforms/warehouse_transforms_project

dbt clean
dbt deps

# Builds the doc files to warehouse_transforms_project/target
dbt docs generate --profiles-dir $WORKSPACE/analytics-secure/warehouse-transforms/ --profile $DBT_PROFILE --target $DBT_TARGET

# Upload the buld docs to S3
pip install awscli

aws s3 cp --recursive ${WORKSPACE}/warehouse-transforms/warehouse_transforms_project/target/ s3://edx-dbt-docs/

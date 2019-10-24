#!/usr/bin/env bash
set -ex

# Setup
cd $WORKSPACE/warehouse-transforms
pip install -r tools/dbt_schema_builder/requirements.txt

cd $WORKSPACE/warehouse-transforms/app_views_project
dbt clean
cd $WORKSPACE/warehouse-transforms/warehouse_transforms_project
dbt clean

cd $WORKSPACE/warehouse-transforms

# Create a new git branch
now=$(date +%d_%m_%Y_%H_%M_%S)
branchname="builder_$now"
git checkout -b "$branchname"

# Run the dbt script to update schemas and sql
cd $WORKSPACE/warehouse-transforms/warehouse_transforms_project
python tools/dbt_schema_builder/schema_builder.py build --raw-schemas $SCHEMAS --profile $DBT_PROFILE --target $DBT_TARGET --profiles-dir $WORKSPACE/analytics-secure/warehouse-transforms/
cd $WORKSPACE/warehouse-transforms/app_views_project
python tools/dbt_schema_builder/schema_builder.py build --raw-schemas $SCHEMAS --profile $DBT_PROFILE --target $DBT_TARGET --profiles-dir $WORKSPACE/analytics-secure/warehouse-transforms/

# Check if any files are added, deleted, or changed. If so, commit them and create a PR.
if [[ -z $(git status -s) ]]
then
  echo "No changes to commit."
else
  # ssh -vT git@github.com
  git config --global user.email "edx-analytics-automation@edx.org"
  git config --global user.name "edX Analytics Automation"

  # Commit all changes to the new branch, making sure new files are added
  git add --all
  git commit --message "Schema Builder automated dbt update at $now"

  # Create a PR on Github from the new branch
  HUB_PROTOCOL=ssh HUB_USER=edx-analytics-automation /snap/bin/hub pull-request --push --no-edit -r edx/edx-data-engineering
fi

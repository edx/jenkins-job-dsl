#!/usr/bin/env bash
set -ex

# Setup
cd $WORKSPACE/warehouse-transforms
pip install dbt-schema-builder

cd $WORKSPACE/warehouse-transforms/projects/$SOURCE_PROJECT
dbt clean --profiles-dir $WORKSPACE/analytics-secure/warehouse-transforms/ --profile $DBT_PROFILE --target $DBT_TARGET
cd $WORKSPACE/warehouse-transforms/projects/$DESTINATION_PROJECT
dbt clean --profiles-dir $WORKSPACE/analytics-secure/warehouse-transforms/ --profile $DBT_PROFILE --target $DBT_TARGET

cd $WORKSPACE/warehouse-transforms

# Create a new git branch
now=$(date +%d_%m_%Y_%H_%M_%S)
branchname="builder_$now"
git checkout -b "$branchname"

# Run the dbt script to update schemas and sql, from the source project directory (necessary for dbt to run)
cd $WORKSPACE/warehouse-transforms/projects/$SOURCE_PROJECT
python dbt_schema_builder build --raw-schemas $SCHEMAS --raw-suffixes $RAW_SUFFIXES --destination-project $DESTINATION_PROJECT --profile $DBT_PROFILE --target $DBT_TARGET --profiles-dir $WORKSPACE/analytics-secure/warehouse-transforms/

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

#!/usr/bin/env bash
set -ex

# Creating python 3.8 virtual environment to run schema builder
PYTHON38_VENV="py38_venv"
virtualenv --python=python3.8 --clear "${PYTHON38_VENV}"
source "${PYTHON38_VENV}/bin/activate"

# Setup
cd $WORKSPACE/warehouse-transforms
pip install --upgrade dbt-schema-builder

source $WORKSPACE/secrets-manager.sh
# Fetch the secrets from AWS
set +x
get_secret_value analytics-secure/warehouse-transforms/profiles DBT_PASSWORD
set -x
export DBT_PASSWORD

cd $WORKSPACE/warehouse-transforms/projects/$SOURCE_PROJECT
dbt clean --profiles-dir $WORKSPACE/warehouse-transforms/profiles/ --profile $DBT_PROFILE --target $DBT_TARGET

# DESTINATION_PROJECT is always relative to SOURCE_PROJECT
cd $DESTINATION_PROJECT
dbt clean --profiles-dir $WORKSPACE/warehouse-transforms/profiles/ --profile $DBT_PROFILE --target $DBT_TARGET

cd $WORKSPACE/warehouse-transforms

# Create a new git branch
now=$(date +%Y_%m_%d_%H_%M_%S)
branchname="builder_$now"
git checkout -b "$branchname"

# Run the dbt script to update schemas and sql, from the source project directory (necessary for dbt to run)
cd $WORKSPACE/warehouse-transforms/projects/$SOURCE_PROJECT
dbt_schema_builder build --destination-project $DESTINATION_PROJECT --profile $DBT_PROFILE --target $DBT_TARGET --profiles-dir $WORKSPACE/warehouse-transforms/profiles/

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
  git commit --message "chore: Schema Builder automated dbt update at $now"

  # Create a PR on Github from the new branch
  HUB_PROTOCOL=ssh /snap/bin/hub pull-request --push --no-edit -r edx/edx-data-engineering
fi

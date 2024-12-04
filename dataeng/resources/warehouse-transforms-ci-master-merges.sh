#!/usr/bin/env bash
set -ex

# Creating python 3.11 virtual environment to run dbt warehouse-transform job
PYTHON311_VENV="py311_venv"
virtualenv --python=python3.11 --clear "${PYTHON311_VENV}"
source "${PYTHON311_VENV}/bin/activate"

# Setup to run python script to create snowflake schema
cd $WORKSPACE/analytics-tools/snowflake
make requirements

# Setup to run dbt commands
cd $WORKSPACE/warehouse-transforms

COMMIT_ID=$(git rev-parse --short HEAD)
# Using --first-parent flag helps to avoids any intermediate commits that developers sometimes leave and merge without squashing
HEAD_COMMIT=$(git log --first-parent origin/master --format='%H' --max-count=1)

# Get second last commit id and compares it with the HEAD to find (git diff) files changed.
PREV_COMMIT_ID=$(git log --first-parent origin/master --format='%H' --max-count=2 | sed -n 2p)

git diff $PREV_COMMIT_ID --name-only

# Finding the project names which has changed between since previous merge commit. Using git diff to compare.
# It returns all the files name with full path. Searching through it using egrep to find which project(s) these changing files belong.
if git diff $HEAD_COMMIT $PREV_COMMIT_ID --name-only | egrep "projects/reporting" -q; then isReporting="true"; else isReporting="false"; fi
if git diff $HEAD_COMMIT $PREV_COMMIT_ID --name-only | egrep "projects/automated/applications" -q; then isApplications="true"; else isApplications="false"; fi
if git diff $HEAD_COMMIT $PREV_COMMIT_ID --name-only | egrep "projects/automated/raw_to_source" -q; then isRawToSource="true"; else isRawToSource="false"; fi
if git diff $HEAD_COMMIT $PREV_COMMIT_ID --name-only | egrep "projects/automated/telemetry" -q; then isTelemetry="true"; else isTelemetry="false"; fi


# Setup to run dbt commands
cd $WORKSPACE/warehouse-transforms

# To install right version of dbt
pip install -r requirements.txt

if [ "$isReporting" == "true" ]
then

    cd $WORKSPACE/analytics-tools/snowflake

    # Schema_Name will be the Github Commit ID e.g. 1724 prefixed with 'merged' and sufixed with project name e.g. 1724_reporting
    export CI_SCHEMA_NAME=merged_${COMMIT_ID}_reporting
    # Schema is dynamically created against each run.
    # profiles.yml contains the name of Schema which is used to create output models when dbt runs.
    python create_ci_schema.py --key_path $KEY_PATH --passphrase_path $PASSPHRASE_PATH --automation_user $USER --account $ACCOUNT --db_name $DB_NAME --schema_name $CI_SCHEMA_NAME
    # create_ci_schema python script not just create schema but also drops the schema if it exists already, and the reason for doing so is if dbt model changes tables that are
    # created in seed job it will fail, so dropping those tables or deleting the whole schema is important to avoid such failure. We noticed while create_ci_schema being running
    # the dbt commands below starts running as they were using different sessions (warehouse and users), in order to complete the drop and create operation before running dbt adding sleep
    sleep 10s
    DBT_PROJECT_PATH='reporting'
    # Full dbt run on merges to master CI (Might decide to run Slim CI in future)
    DBT_RUN_OPTIONS=''
    DBT_RUN_EXCLUDE='' ## Add excluded models here if any
    # Full dbt test on merges to master CI (Might decide to run Slim CI in future)
    DBT_TEST_OPTIONS=''
    DBT_TEST_EXCLUDE=''

    source $WORKSPACE/jenkins-job-dsl/dataeng/resources/warehouse-transforms-ci-dbt.sh

    cd $WORKSPACE/analytics-tools/snowflake
    python remove_ci_schema.py --key_path $KEY_PATH --passphrase_path $PASSPHRASE_PATH --automation_user $USER --account $ACCOUNT --db_name $DB_NAME --schema_name $CI_SCHEMA_NAME

fi


if [ "$isApplications" == "true" ]
then

    cd $WORKSPACE/analytics-tools/snowflake
    export CI_SCHEMA_NAME=merged_${COMMIT_ID}_applications
    python create_ci_schema.py --key_path $KEY_PATH --passphrase_path $PASSPHRASE_PATH --automation_user $USER --account $ACCOUNT --db_name $DB_NAME --schema_name $CI_SCHEMA_NAME

    DBT_PROJECT_PATH='automated/applications'
    DBT_RUN_OPTIONS=''
    DBT_RUN_EXCLUDE=''
    DBT_TEST_OPTIONS=''
    DBT_TEST_EXCLUDE=''

    source $WORKSPACE/jenkins-job-dsl/dataeng/resources/warehouse-transforms-ci-dbt.sh

    cd $WORKSPACE/analytics-tools/snowflake
    python remove_ci_schema.py --key_path $KEY_PATH --passphrase_path $PASSPHRASE_PATH --automation_user $USER --account $ACCOUNT --db_name $DB_NAME --schema_name $CI_SCHEMA_NAME


fi

if [ "$isRawToSource" == "true" ]
then


    cd $WORKSPACE/analytics-tools/snowflake
    export CI_SCHEMA_NAME=merged_${COMMIT_ID}_raw_to_source
    python create_ci_schema.py --key_path $KEY_PATH --passphrase_path $PASSPHRASE_PATH --automation_user $USER --account $ACCOUNT --db_name $DB_NAME --schema_name $CI_SCHEMA_NAME

    DBT_PROJECT_PATH='automated/raw_to_source'
    DBT_RUN_OPTIONS=''
    DBT_RUN_EXCLUDE=''
    DBT_TEST_OPTIONS=''
    DBT_TEST_EXCLUDE=''

    source $WORKSPACE/jenkins-job-dsl/dataeng/resources/warehouse-transforms-ci-dbt.sh

    cd $WORKSPACE/analytics-tools/snowflake
    python remove_ci_schema.py --key_path $KEY_PATH --passphrase_path $PASSPHRASE_PATH --automation_user $USER --account $ACCOUNT --db_name $DB_NAME --schema_name $CI_SCHEMA_NAME


fi


if [ "$isTelemetry" == "true" ]
then
    cd $WORKSPACE/analytics-tools/snowflake
    export CI_SCHEMA_NAME=merged_${COMMIT_ID}_telemetry
    python create_ci_schema.py --key_path $KEY_PATH --passphrase_path $PASSPHRASE_PATH --automation_user $USER --account $ACCOUNT --db_name $DB_NAME --schema_name $CI_SCHEMA_NAME

    DBT_PROJECT_PATH='automated/telemetry'
    DBT_RUN_OPTIONS=''
    DBT_RUN_EXCLUDE=''
    DBT_TEST_OPTIONS=''
    DBT_TEST_EXCLUDE=''

    source $WORKSPACE/jenkins-job-dsl/dataeng/resources/warehouse-transforms-ci-dbt.sh

    cd $WORKSPACE/analytics-tools/snowflake
    python remove_ci_schema.py --key_path $KEY_PATH --passphrase_path $PASSPHRASE_PATH --automation_user $USER --account $ACCOUNT --db_name $DB_NAME --schema_name $CI_SCHEMA_NAME


fi
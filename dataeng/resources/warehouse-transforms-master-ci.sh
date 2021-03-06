#!/usr/bin/env bash
set -ex

# Setup to run python script to create snowflake schema
cd $WORKSPACE/analytics-tools/snowflake
make requirements

# Setup to run dbt commands
cd $WORKSPACE/warehouse-transforms

COMMIT_ID=$(git rev-parse --short HEAD)
# Only run CI on merge commits, otherwise exit
HEAD_COMMIT=$(git rev-parse HEAD)
LAST_MERGE_COMMIT=$(git log --merges origin/master --format='%H' --max-count=1)
if [ $HEAD_COMMIT == $LAST_MERGE_COMMIT ]
then
    echo "This is one of merge commit, Run CI"
else
    echo "Exiting because not a merge commit"
    exit 0
fi 


# Get second last merge commit id and compares it with the HEAD to find (git diff) files changed.
PREV_MERGE_COMMIT_ID=$(git log --merges origin/master --format='%H' --max-count=2 | sed -n 2p)

git diff $PREV_MERGE_COMMIT_ID --name-only

# Finding the project names which has changed between since previous merge commit. Using git diff to compare.
# It returns all the files name with full path. Searching through it using egrep to find which project(s) these changing files belong.
if git diff $PREV_MERGE_COMMIT_ID --name-only | egrep "projects/reporting" -q; then isReporting="true"; else isReporting="false"; fi
if git diff $PREV_MERGE_COMMIT_ID --name-only | egrep "projects/automated/applications" -q; then isApplications="true"; else isApplications="false"; fi
if git diff $PREV_MERGE_COMMIT_ID --name-only | egrep "projects/automated/raw_to_source" -q; then isRawToSource="true"; else isRawToSource="false"; fi
if git diff $PREV_MERGE_COMMIT_ID --name-only | egrep "projects/automated/telemetry" -q; then isTelemetry="true"; else isTelemetry="false"; fi


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
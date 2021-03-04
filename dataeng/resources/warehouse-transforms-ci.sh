#!/usr/bin/env bash
set -ex

# Setup to run python script to create snowflake schema
cd $WORKSPACE/analytics-tools/snowflake
make requirements

# Download Prod build manifest.json file from S3 and creating directory to place manifest file.
cd $WORKSPACE/ && mkdir -p manifest

pip install awscli

aws s3 cp s3://edx-dbt-docs/manifest.json ${WORKSPACE}/manifest

# Setup to run dbt commands
cd $WORKSPACE/warehouse-transforms

BRANCH_POINT=$(git merge-base origin/master ${ghprbActualCommit})
git diff $BRANCH_POINT --name-only

git diff origin/master --name-only

# Finding the project names which has changed in this PR. Using git diff latest to compare this branch from master
# It returns all the files name with full path. Searching through it using egrep to find which project(s) the changing files belong.
# It might happen one PR may be changing files in different projects.
if git diff $BRANCH_POINT --name-only | egrep "projects/reporting" -q; then isReporting="true"; else isReporting="false"; fi
if git diff $BRANCH_POINT --name-only | egrep "projects/automated/applications" -q; then isApplications="true"; else isApplications="false"; fi
if git diff $BRANCH_POINT --name-only | egrep "projects/automated/raw_to_source" -q; then isRawToSource="true"; else isRawToSource="false"; fi
if git diff $BRANCH_POINT --name-only | egrep "projects/automated/telemetry" -q; then isTelemetry="true"; else isTelemetry="false"; fi


# Setup to run dbt commands
cd $WORKSPACE/warehouse-transforms

# To install right version of dbt
pip install -r requirements.txt

if [ "$isReporting" == "true" ]
then

    cd $WORKSPACE/analytics-tools/snowflake

    # Schema_Name will be the Github Pull Request ID e.g. 1724 prefixed with 'PR_*' and sufixed with project name e.g. PR_1724_reporting
    export CI_SCHEMA_NAME=PR_${ghprbPullId}_reporting
    # Schema is dynamically created against each PR. It is the PR number with 'PR_*' as prefixed.
    # profiles.yml contains the name of Schema which is used to create output models when dbt runs. 
    python create_ci_schema.py --key_path $KEY_PATH --passphrase_path $PASSPHRASE_PATH --automation_user $USER --account $ACCOUNT --db_name $DB_NAME --schema_name $CI_SCHEMA_NAME 
    
    DBT_PROJECT_PATH='reporting'
    # This is a Slim CI syntax used to "run" only modified and downstream models
    DBT_RUN_OPTIONS="-m state:modified+ @state:modified --defer --state $WORKSPACE/manifest"
    DBT_RUN_EXCLUDE='' ## Add excluded models here if any
    # This is a Slim CI syntax used to "test" only modified and downstream models
    DBT_TEST_OPTIONS="-m state:modified+ --defer --state $WORKSPACE/manifest"
    DBT_TEST_EXCLUDE='--exclude test_name:relationships' 

    source $WORKSPACE/jenkins-job-dsl/dataeng/resources/warehouse-transforms-ci-dbt.sh

    cd $WORKSPACE/analytics-tools/snowflake
    python remove_ci_schema.py --key_path $KEY_PATH --passphrase_path $PASSPHRASE_PATH --automation_user $USER --account $ACCOUNT --db_name $DB_NAME --schema_name $CI_SCHEMA_NAME 

fi


if [ "$isApplications" == "true" ]
then

    cd $WORKSPACE/analytics-tools/snowflake
    export CI_SCHEMA_NAME=PR_${ghprbPullId}_applications
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
    export CI_SCHEMA_NAME=PR_${ghprbPullId}_raw_to_source
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
    export CI_SCHEMA_NAME=PR_${ghprbPullId}_telemetry
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

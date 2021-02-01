#!/usr/bin/env bash
set -ex

# Setup to run python script to create snowflake schema
cd $WORKSPACE/analytics-tools/snowflake
make requirements
# Schema_Name will be the Github Pull Request ID e.g. 1724 prefixed with 'PR_*' e.g. PR_1724
SCHEMA_NAME=PR_${ghprbPullId}
python create_ci_schema.py \
    --key_path $KEY_PATH \
    --passphrase_path $PASSPHRASE_PATH \
    --automation_user $USER \
    --account $ACCOUNT \
    --db_name $DB_NAME \
    --schema_name $SCHEMA_NAME 

# Schema is dynamically created against each PR. Schema name is the PR number with 'PR_*' as prefixed.
# profiles.yml contains the name of Schema which is used to create output models when dbt runs. Following command
# modifies the schema schema in profiles.yml file  
$(cd $WORKSPACE/analytics-secure/warehouse-transforms/ && sed -i "s/CI_SCHEMA_NAME/$SCHEMA_NAME/g" profiles.yml)


# Download Prod build manifest.json file from S3 and creating directory to place manifest file.
cd $WORKSPACE/ && mkdir -p manifest

pip install awscli

aws s3 cp s3://edx-dbt-docs/manifest.json ${WORKSPACE}/manifest

# Setup to run dbt commands
cd $WORKSPACE/warehouse-transforms

# To install right version of dbt
pip install -r requirements.txt


# Finding the project names which has changed in this PR. Using git diff origin/master to compare this branch from master
# It returns all the files name with full path. Searching through it using egrep to find which project(s) the changing files belong.
# It might happen one PR may be changing files in differen projects.
if [ git diff  origin/master --name-only | egrep "projects/reporting" -q; ]
then
    DBT_PROJECT_PATH='reporting'
    # This is a Slim CI syntax used to "run" only modified and downstream models
    DBT_RUN_OPTIONS='-m state:modified+ @state:modified,1+test_type:data --defer --state $WORKSPACE/manifest' 
    DBT_RUN_EXCLUDE='' ## TODO Add excluded models here
    # Will add --defer here when DBT version is upgraded
    # This is a Slim CI syntax used to "test" only modified and downstream models
    DBT_TEST_OPTIONS='-m state:modified+ --state $WORKSPACE/manifest'
    DBT_TEST_EXCLUDE='--exclude test_name:relationships' 

    cd $WORKSPACE/warehouse-transforms/projects/$DBT_PROJECT_PATH

    dbt clean --profiles-dir $WORKSPACE/analytics-secure/warehouse-transforms/ --profile $DBT_PROFILE --target $DBT_TARGET
    dbt deps --profiles-dir $WORKSPACE/analytics-secure/warehouse-transforms/ --profile $DBT_PROFILE --target $DBT_TARGET
    dbt seed --profiles-dir $WORKSPACE/analytics-secure/warehouse-transforms/ --profile $DBT_PROFILE --target $DBT_TARGET

    dbt run $DBT_RUN_OPTIONS $DBT_RUN_EXCLUDE --profiles-dir $WORKSPACE/analytics-secure/warehouse-transforms/ --profile $DBT_PROFILE --target $DBT_TARGET
    dbt test $DBT_TEST_OPTIONS $DBT_TEST_EXCLUDE --profiles-dir $WORKSPACE/analytics-secure/warehouse-transforms/ --profile $DBT_PROFILE --target $DBT_TARGET

    # For 'daily' models, exclude these models from being run or tested
    # exclude_models="$exclude_models finrep_map_organization_course_courserun finrep_royalty_order_dimension tag:daily-exclude"
fi


if [ git diff  origin/master --name-only | egrep "projects/automated/applications" -q; ]
then
    DBT_PROJECT_PATH='automated/applications'
    DBT_RUN_OPTIONS='' # It will be a full dbt run for automated
    DBT_RUN_EXCLUDE=''
    DBT_TEST_OPTIONS='' # It will be a full dbt test for automated
    DBT_TEST_EXCLUDE=''

    cd $WORKSPACE/warehouse-transforms/projects/$DBT_PROJECT_PATH

    dbt clean --profiles-dir $WORKSPACE/analytics-secure/warehouse-transforms/ --profile $DBT_PROFILE --target $DBT_TARGET
    dbt deps --profiles-dir $WORKSPACE/analytics-secure/warehouse-transforms/ --profile $DBT_PROFILE --target $DBT_TARGET
    dbt seed --profiles-dir $WORKSPACE/analytics-secure/warehouse-transforms/ --profile $DBT_PROFILE --target $DBT_TARGET

    dbt run $DBT_RUN_OPTIONS $DBT_RUN_EXCLUDE --profiles-dir $WORKSPACE/analytics-secure/warehouse-transforms/ --profile $DBT_PROFILE --target $DBT_TARGET
    dbt test $DBT_TEST_OPTIONS $DBT_TEST_EXCLUDE --profiles-dir $WORKSPACE/analytics-secure/warehouse-transforms/ --profile $DBT_PROFILE --target $DBT_TARGET

fi

if [ git diff  origin/master --name-only | egrep "projects/automated/raw_to_source" -q; ]
then
    DBT_PROJECT_PATH='automated/raw_to_source'
    DBT_RUN_OPTIONS=''
    DBT_RUN_EXCLUDE=''
    DBT_TEST_OPTIONS=''
    DBT_TEST_EXCLUDE=''

    cd $WORKSPACE/warehouse-transforms/projects/$DBT_PROJECT_PATH

    dbt clean --profiles-dir $WORKSPACE/analytics-secure/warehouse-transforms/ --profile $DBT_PROFILE --target $DBT_TARGET
    dbt deps --profiles-dir $WORKSPACE/analytics-secure/warehouse-transforms/ --profile $DBT_PROFILE --target $DBT_TARGET
    dbt seed --profiles-dir $WORKSPACE/analytics-secure/warehouse-transforms/ --profile $DBT_PROFILE --target $DBT_TARGET

    dbt run $DBT_RUN_OPTIONS $DBT_RUN_EXCLUDE --profiles-dir $WORKSPACE/analytics-secure/warehouse-transforms/ --profile $DBT_PROFILE --target $DBT_TARGET
    dbt test $DBT_TEST_OPTIONS $DBT_TEST_EXCLUDE --profiles-dir $WORKSPACE/analytics-secure/warehouse-transforms/ --profile $DBT_PROFILE --target $DBT_TARGET

fi


if [ git diff  origin/master --name-only | egrep "projects/automated/telemetry" -q; ]
then
    DBT_PROJECT_PATH='automated/telemetry'
    DBT_RUN_OPTIONS=''
    DBT_RUN_EXCLUDE=''
    DBT_TEST_OPTIONS=''
    DBT_TEST_EXCLUDE=''

    cd $WORKSPACE/warehouse-transforms/projects/$DBT_PROJECT_PATH

    dbt clean --profiles-dir $WORKSPACE/analytics-secure/warehouse-transforms/ --profile $DBT_PROFILE --target $DBT_TARGET
    dbt deps --profiles-dir $WORKSPACE/analytics-secure/warehouse-transforms/ --profile $DBT_PROFILE --target $DBT_TARGET
    dbt seed --profiles-dir $WORKSPACE/analytics-secure/warehouse-transforms/ --profile $DBT_PROFILE --target $DBT_TARGET

    dbt run $DBT_RUN_OPTIONS $DBT_RUN_EXCLUDE --profiles-dir $WORKSPACE/analytics-secure/warehouse-transforms/ --profile $DBT_PROFILE --target $DBT_TARGET
    dbt test $DBT_TEST_OPTIONS $DBT_TEST_EXCLUDE --profiles-dir $WORKSPACE/analytics-secure/warehouse-transforms/ --profile $DBT_PROFILE --target $DBT_TARGET

fi

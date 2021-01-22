#!/usr/bin/env bash
set -ex
# This is work in progress script 
# Need to work on snowflake schema and correct profile

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

###################################################################################################################3
# Download Prod build manifest.json file from S3 and creating directory to place manifest file.
$(cd $WORKSPACE/ && mkdir manifest)

pip install awscli

aws s3 cp --recursive s3://edx-dbt-docs/manifest.json ${WORKSPACE}/manifest

# Setup to run dbt commands
cd $WORKSPACE/warehouse-transforms

# To install right version of dbt
pip install -r requirements.txt

cd $WORKSPACE/warehouse-transforms/projects/$DBT_PROJECT_PATH

# Give target CI_TESTS
dbt clean --profiles-dir $WORKSPACE/analytics-secure/warehouse-transforms/ --profile $DBT_PROFILE --target $DBT_TARGET
dbt deps --profiles-dir $WORKSPACE/analytics-secure/warehouse-transforms/ --profile $DBT_PROFILE --target $DBT_TARGET
dbt seed --profiles-dir $WORKSPACE/analytics-secure/warehouse-transforms/ --profile $DBT_PROFILE --target $DBT_TARGET

# WIP: Running on only google analytics sessions models tag to speed up the test runs
####dbt test --models tag:google_analytics_sessions --exclude 'source:*' --profile warehouse_transforms --target $DBT_TARGET --profiles-dir $WORKSPACE/analytics-secure/warehouse-transforms/
#dbt test $DBT_RUN_OPTIONS --profile $DBT_PROFILE --target $DBT_TARGET --profiles-dir $WORKSPACE/analytics-secure/warehouse-transforms/


dbt run -m state:modified+ --defer --state ${WORKSPACE}/manifest --profiles-dir $WORKSPACE/analytics-secure/warehouse-transforms/ --profile $DBT_PROFILE --target $DBT_TARGET

dbt test -m state:modified+ --state ${WORKSPACE}/manifest --profiles-dir $WORKSPACE/analytics-secure/warehouse-transforms/ --profile $DBT_PROFILE --target $DBT_TARGET
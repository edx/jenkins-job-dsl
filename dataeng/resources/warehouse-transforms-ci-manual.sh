#!/usr/bin/env bash
set -ex

# Creating python 3.8 virtual environment to run dbt warehouse-transform job
PYTHON38_VENV="py38_venv"
virtualenv --python=python3.8 --clear "${PYTHON38_VENV}"
source "${PYTHON38_VENV}/bin/activate"

# Specifying GITHUB_PR_ID and WAREHOUSE_TRANSFORMS_BRANCH is a must
if [[ "$GITHUB_PR_ID" == "" || "$WAREHOUSE_TRANSFORMS_BRANCH" == "" ]]
then
    echo "Please provide GITHUB_PR_ID and WAREHOUSE_TRANSFORMS_BRANCH"
    exit 1
fi    

# Setup to run python script to create snowflake schema
cd $WORKSPACE/analytics-tools/snowflake
make requirements

# Download Prod build manifest.json file from S3 and creating directory to place manifest file.
cd $WORKSPACE/ && mkdir -p manifest

pip install awscli

aws s3 cp s3://edx-dbt-docs/manifest.json ${WORKSPACE}/manifest

# Find the project name to append with CI_SCHEMA_NAME
if echo $DBT_PROJECT_PATH | egrep "reporting" -q; then PROJECT_NAME="reporting"; fi
if echo $DBT_PROJECT_PATH | egrep "automated/applications" -q; then PROJECT_NAME="automated/applications"; fi
if echo $DBT_PROJECT_PATH | egrep "automated/raw_to_source" -q; then PROJECT_NAME="automated/raw_to_source"; fi
if echo $DBT_PROJECT_PATH | egrep "automated/telemetry" -q; then PROJECT_NAME="automated/telemetry"; fi


# Setup to run dbt commands
cd $WORKSPACE/warehouse-transforms

# To install right version of dbt
pip install -r requirements.txt

cd $WORKSPACE/analytics-tools/snowflake
export CI_SCHEMA_NAME=PR_${GITHUB_PR_ID}_${PROJECT_NAME}
# Schema is dynamically created against each run.
# profiles.yml contains the name of Schema which is used to create output models when dbt runs.
python create_ci_schema.py --key_path $KEY_PATH --passphrase_path $PASSPHRASE_PATH --automation_user $USER --account $ACCOUNT --db_name $DB_NAME --schema_name $CI_SCHEMA_NAME --no_drop_old

cd $WORKSPACE/warehouse-transforms/projects/$DBT_PROJECT_PATH

dbt clean --profiles-dir $WORKSPACE/analytics-secure/warehouse-transforms/ --profile $DBT_PROFILE --target $DBT_TARGET
dbt deps --profiles-dir $WORKSPACE/analytics-secure/warehouse-transforms/ --profile $DBT_PROFILE --target $DBT_TARGET
dbt seed --profiles-dir $WORKSPACE/analytics-secure/warehouse-transforms/ --profile $DBT_PROFILE --target $DBT_TARGET

if [[ "$RUN_TESTS_ONLY" != "true" ]]
then
    dbt run $DBT_RUN_OPTIONS $DBT_RUN_EXCLUDE --profiles-dir $WORKSPACE/analytics-secure/warehouse-transforms/ --profile $DBT_PROFILE --target $DBT_TARGET
fi

dbt test $DBT_TEST_OPTIONS $DBT_TEST_EXCLUDE --profiles-dir $WORKSPACE/analytics-secure/warehouse-transforms/ --profile $DBT_PROFILE --target $DBT_TARGET
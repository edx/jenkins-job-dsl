#!/usr/bin/env bash
set -ex

# Creating Python virtual env
PYTHON_VENV="python_venv"
virtualenv --python=$PYTHON_VENV_VERSION --clear "${PYTHON_VENV}"
source "${PYTHON_VENV}/bin/activate"

# Setup
cd $WORKSPACE/analytics-tools/snowflake
make requirements

# Source the secrets-manager.sh script to make the function available
source $WORKSPACE/secrets-manager.sh
# Fetch the secrets from AWS
set +x
get_secret_value analytics-secure/job-configs/SNOWFLAKE_REFRESH_SNOWPIPE_JOB_EXTRA_VARS KEY_PATH
get_secret_value analytics-secure/job-configs/SNOWFLAKE_REFRESH_SNOWPIPE_JOB_EXTRA_VARS PASSPHRASE_PATH
get_secret_value analytics-secure/job-configs/SNOWFLAKE_REFRESH_SNOWPIPE_JOB_EXTRA_VARS USER
get_secret_value analytics-secure/job-configs/SNOWFLAKE_REFRESH_SNOWPIPE_JOB_EXTRA_VARS ACCOUNT
set -x

python refresh_snowpipe.py \
    --key_path $WORKSPACE/analytics-secure/$KEY_PATH \
    --passphrase_path $WORKSPACE/analytics-secure/$PASSPHRASE_PATH \
    --user $USER \
    --schema $SCHEMA \
    --account $ACCOUNT \
    --pipe_name $PIPE_NAME \
    --table_name $TABLE_NAME \
    --delay $DELAY \
    --limit $LIMIT

#!/usr/bin/env bash
set -ex

# Creating Python virtual env
PYTHON_VENV="python_venv"
virtualenv --python=$PYTHON_VENV_VERSION --clear "${PYTHON_VENV}"
source "${PYTHON_VENV}/bin/activate"

# Setup
cd $WORKSPACE/analytics-tools/snowflake
make requirements

source secrets-manager.sh analytics-secure/job-configs/SNOWFLAKE_REFRESH_SNOWPIPE_JOB_EXTRA_VARS KEY_PATH
source secrets-manager.sh analytics-secure/job-configs/SNOWFLAKE_REFRESH_SNOWPIPE_JOB_EXTRA_VARS PASSPHRASE_PATH
source secrets-manager.sh analytics-secure/job-configs/SNOWFLAKE_REFRESH_SNOWPIPE_JOB_EXTRA_VARS USER
source secrets-manager.sh analytics-secure/job-configs/SNOWFLAKE_REFRESH_SNOWPIPE_JOB_EXTRA_VARS ACCOUNT
source secrets-manager.sh analytics-secure/job-configs/SNOWFLAKE_REFRESH_SNOWPIPE_JOB_EXTRA_VARS JOB_FREQUENCY

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

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


secrets-manager.sh -w analytics-secure/job-configs/SNOWFLAKE_REFRESH_SNOWPIPE_JOB_EXTRA_VARS snowflake/rsa_key_snowpipe_user.p8
secrets-manager.sh -w analytics-secure/job-configs/SNOWFLAKE_REFRESH_SNOWPIPE_JOB_EXTRA_VARS snowflake/rsa_key_passphrase_snowpipe_user

set -x

python refresh_snowpipe.py \
    --user 'SNOWPIPE' \
    --schema $SCHEMA \
    --account 'edx.us-east-1' \
    --pipe_name $PIPE_NAME \
    --table_name $TABLE_NAME \
    --delay $DELAY \
    --limit $LIMIT
    --key_file $KEY_PATH \
    --passphrase_file $PASSPHRASE_PATH

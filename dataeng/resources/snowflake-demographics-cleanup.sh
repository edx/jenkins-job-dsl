#!/usr/bin/env bash
set -ex

# Creating Python virtual env
PYTHON_VENV="python_venv"
virtualenv --python=$PYTHON_VENV_VERSION --clear "${PYTHON_VENV}"
source "${PYTHON_VENV}/bin/activate"

# Setup
cd $WORKSPACE/analytics-tools/snowflake
make requirements

source secrets-manager.sh analytics-secure/job-configs/SNOWFLAKE_DEMOGRAPHICS_CLEANUP_JOB_EXTRA_VARS KEY_PATH
source secrets-manager.sh analytics-secure/job-configs/SNOWFLAKE_DEMOGRAPHICS_CLEANUP_JOB_EXTRA_VARS PASSPHRASE_PATH
source secrets-manager.sh analytics-secure/job-configs/SNOWFLAKE_DEMOGRAPHICS_CLEANUP_JOB_EXTRA_VARS USER
source secrets-manager.sh analytics-secure/job-configs/SNOWFLAKE_DEMOGRAPHICS_CLEANUP_JOB_EXTRA_VARS ACCOUNT

python demographics_cleanup.py \
    --key_path $WORKSPACE/analytics-secure/$KEY_PATH \
    --passphrase_path $WORKSPACE/analytics-secure/$PASSPHRASE_PATH \
    --user $USER \
    --account $ACCOUNT

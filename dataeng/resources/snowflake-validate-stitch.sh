#!/usr/bin/env bash
set -ex

# Creating Python virtual env
PYTHON_VENV="python_venv"
virtualenv --python=$PYTHON_VENV_VERSION --clear "${PYTHON_VENV}"
source "${PYTHON_VENV}/bin/activate"

# Calculate the start of the validation window as 15 days prior to the end of the window.
COMPARISON_END_TIME="${SQOOP_START_TIME}"
COMPARISON_START_TIME=$(date --utc --iso=minutes -d "${COMPARISON_END_TIME} - 15 days")

# Tooling setup
cd $WORKSPACE/analytics-tools/snowflake
make requirements

source secrets-manager.sh analytics-secure/job-configs/SNOWFLAKE_VALIDATE_STITCH_JOB_EXTRA_VARS KEY_PATH
source secrets-manager.sh analytics-secure/job-configs/SNOWFLAKE_VALIDATE_STITCH_JOB_EXTRA_VARS PASSPHRASE_PATH
source secrets-manager.sh analytics-secure/job-configs/SNOWFLAKE_VALIDATE_STITCH_JOB_EXTRA_VARS USER
source secrets-manager.sh analytics-secure/job-configs/SNOWFLAKE_VALIDATE_STITCH_JOB_EXTRA_VARS ACCOUNT

python stitch_vs_sqoop_validation.py \
    --key_path $WORKSPACE/analytics-secure/${SNOWFLAKE_KEY_PATH} \
    --passphrase_path $WORKSPACE/analytics-secure/${SNOWFLAKE_PASSPHRASE_PATH} \
    --user ${SNOWFLAKE_USER} \
    --account ${SNOWFLAKE_ACCOUNT} \
    --schema ${APP_NAME} \
    --begin_datetime ${COMPARISON_START_TIME} \
    --end_datetime ${COMPARISON_END_TIME}

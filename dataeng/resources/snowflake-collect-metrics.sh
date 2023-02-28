#!/usr/bin/env bash
set -ex

# Creating Python virtual env
PYTHON_VENV="python_venv"
virtualenv --python=$PYTHON_VENV_VERSION --clear "${PYTHON_VENV}"
source "${PYTHON_VENV}/bin/activate"

# Setup
cd $WORKSPACE/analytics-tools/snowflake
make requirements

python collect-metrics.py \
    --metric_name $METRIC_NAME \
    --key_path $WORKSPACE/analytics-secure/snowflake/rsa_key_snowflake_task_automation_user.p8 \
    --passphrase_path $WORKSPACE/analytics-secure/snowflake/rsa_key_passphrase_snowflake_task_automation_user \
    --automation_user $SNOWFLAKE_USER \
    --account $SNOWFLAKE_ACCOUNT \
    --warehouse $SNOWFLAKE_WAREHOUSE

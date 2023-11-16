#!/usr/bin/env bash
set -ex

# Creating Python virtual env
PYTHON_VENV="python_venv"
virtualenv --python=$PYTHON_VENV_VERSION --clear "${PYTHON_VENV}"
source "${PYTHON_VENV}/bin/activate"

# Setup
cd $WORKSPACE/analytics-tools/snowflake
make requirements

source secrets-manager.sh analytics-secure/job-configs/SNOWFLAKE_COLLECT_METRICS_JOB_EXTRA_VARS KEY_PATH
source secrets-manager.sh analytics-secure/job-configs/SNOWFLAKE_COLLECT_METRICS_JOB_EXTRA_VARS PASSPHRASE_PATH
source secrets-manager.sh analytics-secure/job-configs/SNOWFLAKE_COLLECT_METRICS_JOB_EXTRA_VARS USER
source secrets-manager.sh analytics-secure/job-configs/SNOWFLAKE_COLLECT_METRICS_JOB_EXTRA_VARS ACCOUNT
source secrets-manager.sh analytics-secure/job-configs/SNOWFLAKE_COLLECT_METRICS_JOB_EXTRA_VARS METRIC_NAME

python collect-metrics.py \
    --metric_name $METRIC_NAME \
    --key_path $WORKSPACE/analytics-secure/snowflake/rsa_key_snowflake_task_automation_user.p8 \
    --passphrase_path $WORKSPACE/analytics-secure/snowflake/rsa_key_passphrase_snowflake_task_automation_user \
    --automation_user $SNOWFLAKE_USER \
    --account $SNOWFLAKE_ACCOUNT \
    --warehouse $SNOWFLAKE_WAREHOUSE

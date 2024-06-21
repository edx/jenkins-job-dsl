#!/usr/bin/env bash
set -ex

# Creating Python virtual env
PYTHON_VENV="python_venv"
virtualenv --python=$PYTHON_VENV_VERSION --clear "${PYTHON_VENV}"
source "${PYTHON_VENV}/bin/activate"

# Setup
cd $WORKSPACE/analytics-tools/snowflake
make requirements

python3 secrets-manager.py -w -n analytics-secure/snowflake/rsa_key_snowflake_task_automation_user.p8 -v rsa_key_snowflake_task_automation_user
python3 secrets-manager.py -w -n analytics-secure/snowflake/rsa_key_passphrase_snowflake_task_automation_user -v rsa_key_passphrase_snowflake_task_automation_user



python collect-metrics.py \
    --metric_name $METRIC_NAME \
    --automation_user $SNOWFLAKE_USER \
    --account $SNOWFLAKE_ACCOUNT \
    --warehouse $SNOWFLAKE_WAREHOUSE \
    --key_file rsa_key_snowflake_task_automation_user \
    --passphrase_file rsa_key_passphrase_snowflake_task_automation_user


rm rsa_key_snowflake_task_automation_user
rm rsa_key_passphrase_snowflake_task_automation_user

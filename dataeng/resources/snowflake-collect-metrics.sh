#!/usr/bin/env bash
set -ex

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

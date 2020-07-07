#!/usr/bin/env bash
set -ex

# Setup
cd $WORKSPACE/analytics-tools/snowflake
make requirements

python collect_queue_depth.py \
    --key_path $WORKSPACE/analytics-secure/snowflake/rsa_key_snowflake_task_automation_user.p8 \
    --passphrase_path $WORKSPACE/analytics-secure/snowflake/rsa_key_passphrase_snowflake_task_automation_user \
    --automation_user "SNOWFLAKE_TASK_AUTOMATION_USER" \
    --account "edx.us-east-1"

#!/usr/bin/env bash
set -ex

# Setup
cd $WORKSPACE/analytics-tools/snowflake
make requirements

python collect_queue_depth.py \
    --key_path $WORKSPACE/analytics-secure/$KEY_PATH \
    --passphrase_path $WORKSPACE/analytics-secure/$PASSPHRASE_PATH \
    --automation_user $WORKSPACE/analytics-secure/$SNOWFLAKE_USER \
    --account $WORKSPACE/analytics-secure/$SNOWFLAKE_ACCOUNT

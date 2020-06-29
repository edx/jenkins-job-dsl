#!/usr/bin/env bash
set -ex

# Setup
cd $WORKSPACE/analytics-tools/snowflake
make requirements

python collect_queue_depth.py \
    --key_path $KEY_PATH \
    --passphrase_path $PASSPHRASE_PATH \
    --automation_user $USER \
    --account $ACCOUNT
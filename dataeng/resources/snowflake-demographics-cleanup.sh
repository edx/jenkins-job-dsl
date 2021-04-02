#!/usr/bin/env bash
set -ex

# Setup
cd $WORKSPACE/analytics-tools/snowflake
make requirements

python demographics_cleanup.py \
    --key_path $WORKSPACE/analytics-secure/$KEY_PATH \
    --passphrase_path $WORKSPACE/analytics-secure/$PASSPHRASE_PATH \
    --user $USER \
    --account $ACCOUNT

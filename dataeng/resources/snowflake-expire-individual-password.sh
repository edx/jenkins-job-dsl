#!/usr/bin/env bash
set -ex

# Setup
cd $WORKSPACE/analytics-tools/snowflake
make requirements

python expire_user_passwords.py \
    --key_path $KEY_PATH \
    --passphrase_path $PASSPHRASE_PATH \
    --automation_user $USER \
    --account $ACCOUNT \
    --user_to_expire $USER_TO_EXPIRE

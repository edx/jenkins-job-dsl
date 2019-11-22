#!/usr/bin/env bash
set -ex

# Setup
cd $WORKSPACE/analytics-tools/snowflake
make requirements

python refresh_snowpipe.py \
    --key_path $KEY_PATH \
    --passphrase_path $PASSPHRASE_PATH \
    --user $USER \
    --account $ACCOUNT \
    --pipe_name $PIPE_NAME \
    --table_name $TABLE_NAME \
    --delay $DELAY \
    --limit $LIMIT

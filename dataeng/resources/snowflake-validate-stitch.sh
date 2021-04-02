#!/usr/bin/env bash
set -ex

# Calculate the start of the validation window as 15 days prior to the end of the window.
COMPARISON_END_TIME="${SQOOP_START_TIME}"
COMPARISON_START_TIME=$(date --utc --iso=minutes -d "${COMPARISON_END_TIME} - 15 days")

# Tooling setup
cd $WORKSPACE/analytics-tools/snowflake
make requirements

python stitch_vs_sqoop_validation.py \
    --key_path $WORKSPACE/analytics-secure/${SNOWFLAKE_KEY_PATH} \
    --passphrase_path $WORKSPACE/analytics-secure/${SNOWFLAKE_PASSPHRASE_PATH} \
    --user ${SNOWFLAKE_USER} \
    --account ${SNOWFLAKE_ACCOUNT} \
    --schema ${APP_NAME} \
    --begin_datetime ${COMPARISON_START_TIME} \
    --end_datetime ${COMPARISON_END_TIME}

#!/usr/bin/env bash
set -ex

# Calculate the start of the validation window as 10 days prior to the end of the window.
COMPARISON_END_TIME="${SQOOP_START_TIME}"
COMPARISON_START_TIME=$(date -d "${COMPARISON_END_TIME} - 10 days")

# Tooling setup
cd $WORKSPACE/analytics-tools/snowflake
make requirements

python stitch_vs_sqoop_validation.py \
    --key_path ${KEY_PATH} \
    --passphrase_path ${PASSPHRASE_PATH} \
    --user ${USER} \
    --account ${ACCOUNT} \
    --schema ${APP_NAME} \
    --begin_datetime ${COMPARISON_START_TIME} \
    --end_datetime ${COMPARISON_END_TIME}

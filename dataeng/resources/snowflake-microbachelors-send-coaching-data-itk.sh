#!/usr/bin/env bash
set -ex

# setup
cd $WORKSPACE/analytics-tools/snowflake
pip install -r requirements/microbachelors.txt

# run the script twice to generate student and course reports for ITK
python send_coaching_data_itk.py \
    --key_path $KEY_PATH \
    --passphrase_path $PASSPHRASE_PATH \
    --user $USER \
    --account $ACCOUNT \
    --report-type student \
    --send False

python send_coaching_data_itk.py \
    --key_path $KEY_PATH \
    --passphrase_path $PASSPHRASE_PATH \
    --user $USER \
    --account $ACCOUNT \
    --report-type course \
    --send False

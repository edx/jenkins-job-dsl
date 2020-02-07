#!/usr/bin/env bash
set -ex

# setup
cd $WORKSPACE/analytics-tools/snowflake
pip install -r requirements/microbachelors.txt

# Run the script twice. Once for the student report, then again for the Course report
python send_coaching_data_itk.py \
    --key_path $KEY_PATH \
    --passphrase_path $PASSPHRASE_PATH \
    --user $USER \
    --account $ACCOUNT \
    --report-type student \
    --send True

python send_coaching_data_itk.py \
    --key_path $KEY_PATH \
    --passphrase_path $PASSPHRASE_PATH \
    --user $USER \
    --account $ACCOUNT \
    --report-type course \
    --send True

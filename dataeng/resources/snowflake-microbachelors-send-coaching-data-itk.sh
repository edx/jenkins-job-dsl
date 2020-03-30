#!/usr/bin/env bash
set -ex

# setup
cd $WORKSPACE/analytics-tools/snowflake
pip install -r requirements/microbachelors.txt

# download the SFTP credentials from S3
aws s3 cp $SFTP_CREDENTIALS_BUCKET $WORKSPACE/

# run the script twice to generate student and course reports for ITK
python send_coaching_data_itk.py \
    --key_path $KEY_PATH \
    --passphrase_path $PASSPHRASE_PATH \
    --user $USER \
    --account $ACCOUNT \
    --report_type student \
    --send True \
    --sftp_credentials_file $WORKSPACE/itk_sftp.json \
    --sftp_path $SFTP_STUDENT_PATH

python send_coaching_data_itk.py \
    --key_path $KEY_PATH \
    --passphrase_path $PASSPHRASE_PATH \
    --user $USER \
    --account $ACCOUNT \
    --report_type course \
    --send True \
    --sftp_credentials_file $WORKSPACE/itk_sftp.json \
    --sftp_path $SFTP_COURSE_PATH

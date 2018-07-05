#!/usr/bin/env bash

set -ex

# prepare credentials
mkdir -p $WORKSPACE/user-retirement-secure
cp $USER_RETIREMENT_SECURE_DEFAULT $WORKSPACE/user-retirement-secure/secure-default.yml

# prepare tubular
cd $WORKSPACE/tubular
pip install -r requirements.txt

# Create the directory where we will store reports, one per partner
rm -rf $PARTNER_REPORTS_DIR
mkdir $PARTNER_REPORTS_DIR

# Call the script to generate the reports and upload them to Google Drive
python scripts/retirement_partner_report.py \
    --config_file=$WORKSPACE/user-retirement-secure/$ENVIRONMENT.yml \
    --google_secrets_file=$WORKSPACE/user-retirement-secure/google-service-accounts/service-account-$ENVIRONMENT.json \
    --output_dir=$PARTNER_REPORTS_DIR

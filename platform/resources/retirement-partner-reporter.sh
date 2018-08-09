#!/usr/bin/env bash

set -ex

# Display the environment variables again, this time within the context of a
# new subshell inside of the job. N.B. this should not print plain credentials
# because the only credentialsBindings we currently use is of type "file" which
# just stores a filename in the environment (rather than the content).
env

# Make sure that when we try to write unicode to the console, it
# correctly encodes to UTF-8 rather than exiting with a UnicodeEncode
# error.
export PYTHONIOENCODING=UTF-8
export LC_CTYPE=en_US.UTF-8

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

#!/usr/bin/env bash

set -ex

# prepare credentials
mkdir -p $WORKSPACE/user-retirement-secure
cp $USER_RETIREMENT_SECURE_DEFAULT $WORKSPACE/user-retirement-secure/secure-default.yml

# prepare tubular
cd $WORKSPACE/tubular
# match versions of pip and setuptools installed as part of tubular CI.
pip install 'pip==20.3.3' 'setuptools==50.3.2'
pip install -r requirements.txt

# Call the script to cleanup the reports
python scripts/delete_expired_partner_gdpr_reports.py \
    --config_file=$WORKSPACE/user-retirement-secure/$ENVIRONMENT.yml \
    --google_secrets_file=$WORKSPACE/user-retirement-secure/google-service-accounts/service-account-$ENVIRONMENT.json \
    --age_in_days=$AGE_IN_DAYS

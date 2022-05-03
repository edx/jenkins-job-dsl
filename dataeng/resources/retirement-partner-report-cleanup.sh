#!/usr/bin/env bash

set -ex

# Create and activate a virtualenv.  In case we ever change the concurrency
# setting on the jenkins worker, it would be safest to keep the builds from
# clobbering each other's virtualenvs.
VENV="venv-${BUILD_NUMBER}"
virtualenv --python=python3.8 --clear "${VENV}"
source "${VENV}/bin/activate"


# prepare tubular
cd $WORKSPACE/tubular
# snapshot the current latest versions of pip and setuptools.
pip install 'pip==21.0.1' 'setuptools==53.0.0'
pip install -r requirements.txt

# Call the script to cleanup the reports
# This section is for long term use
# python scripts/delete_expired_partner_gdpr_reports.py \
#     --config_file=$WORKSPACE/user-retirement-secure/$ENVIRONMENT.yml \
#     --google_secrets_file=$WORKSPACE/user-retirement-secure/google-service-accounts/service-account-$ENVIRONMENT.json \
#     --age_in_days=$AGE_IN_DAYS
# This line is for use until 2022-05-13. Please make sure to uncomment
# the script invocation above to make sure we revert back to using service accounts.
# Please see DENG-1215 for more information
python scripts/delete_expired_partner_gdpr_reports.py \
    --config_file=$WORKSPACE/user-retirement-secure/$ENVIRONMENT.yml \
    --google_secrets_file=$WORKSPACE/user-retirement-secure/google-service-accounts/no-reply-user-oauth2-token.json \
    --age_in_days=$AGE_IN_DAYS \
    --as_user_account

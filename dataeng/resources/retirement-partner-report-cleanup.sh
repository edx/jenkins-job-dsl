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
python scripts/delete_expired_partner_gdpr_reports.py \
    --config_file=$WORKSPACE/user-retirement-secure/$ENVIRONMENT.yml \
    --google_secrets_file=$WORKSPACE/user-retirement-secure/google-service-accounts/service-account-$ENVIRONMENT.json \
    --age_in_days=$AGE_IN_DAYS

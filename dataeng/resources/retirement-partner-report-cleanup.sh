#!/usr/bin/env bash

set -ex

# Create and activate a virtualenv.  In case we ever change the concurrency
# setting on the jenkins worker, it would be safest to keep the builds from
# clobbering each other's virtualenvs.
VENV="venv-${BUILD_NUMBER}"
virtualenv --python=python3.9 --clear "${VENV}"
source "${VENV}/bin/activate"

#Fetch secrets from AWS
cd $WORKSPACE/configuration
pip install -r util/jenkins/requirements.txt
# hide the sensitive information in the logs
set +x

CONFIG_YAML=$(aws secretsmanager get-secret-value --secret-id "user-retirement-secure/$ENVIRONMENT" --region "us-east-1" --output json | jq -r '.SecretString' | yq -y .)

# Create a temporary file to store the YAML
TEMP_CONFIG_YAML=$(mktemp $WORKSPACE/tempfile.XXXXXXXXXX.yml)

# Write the YAML data to the temporary file
echo "$CONFIG_YAML" > "$TEMP_CONFIG_YAML"

# Fetch google-service-account secrets
GOOGLE_SERVICE_ACCOUNT_JSON=$(aws secretsmanager get-secret-value --secret-id "user-retirement-secure/google-service-accounts/service-account-$ENVIRONMENT.json" --region "us-east-1" --output json | jq -r '.SecretString')
# Create a temporary file to store the YAML
TEMP_GOOGLE_SECRETS=$(mktemp $WORKSPACE/tempfile.XXXXXXXXXX.json)

# Write the YAML data to the temporary file
echo "$GOOGLE_SERVICE_ACCOUNT_JSON" > "$TEMP_GOOGLE_SECRETS"

set -x

# prepare tubular
cd $WORKSPACE/tubular
# snapshot the current latest versions of pip and setuptools.
pip install 'pip==21.0.1' 'setuptools==53.0.0'
pip install -r requirements.txt

# Call the script to cleanup the reports
python scripts/delete_expired_partner_gdpr_reports.py \
    --config_file=$TEMP_CONFIG_YAML \
    --google_secrets_file=$TEMP_GOOGLE_SECRETS \
    --age_in_days=$AGE_IN_DAYS

# Remove the temporary files after processing
rm -f "$TEMP_CONFIG_YAML"
rm -f "$TEMP_GOOGLE_SECRETS"

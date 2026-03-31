#!/usr/bin/env bash

set -ex

# Display the environment variables again, this time within the context of a
# new subshell inside of the job. N.B. this should not print plain credentials
# because the only credentialsBindings we currently use is of type "file" which
# just stores a filename in the environment (rather than the content).
env

# Create and activate a virtualenv.  In case we ever change the concurrency
# setting on the jenkins worker, it would be safest to keep the builds from
# clobbering each other's virtualenvs.
VENV="venv-${BUILD_NUMBER}"
virtualenv --python=python3.9 --clear "${VENV}"
source "${VENV}/bin/activate"

# Make sure that when we try to write unicode to the console, it
# correctly encodes to UTF-8 rather than exiting with a UnicodeEncode
# error.
export PYTHONIOENCODING=UTF-8
export LC_CTYPE=en_US.UTF-8

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

# Load public configuration from edx-internal Git repo
cd $WORKSPACE/edx-internal
ENABLE_CHECK_EXPIRING_FILES=$(yq -r ".PARTNER_REPORTER_VARS[] | select(.ENVIRONMENT_DEPLOYMENT == \"${ENVIRONMENT}\") | .ENABLE_CHECK_EXPIRING_FILES // false" \
    tools-edx-jenkins/user-retirement.yml)

# prepare tubular
cd $WORKSPACE/tubular
# snapshot the current latest versions of pip and setuptools.
pip install 'pip==21.0.1' 'setuptools==53.0.0'
pip install -r requirements.txt

# Create the directory where we will store reports, one per partner
rm -rf $PARTNER_REPORTS_DIR
mkdir $PARTNER_REPORTS_DIR

# Validate and set defaults for Jenkins parameters
# AGE_IN_DAYS: defaults to 60 if not provided
if [[ -z "${AGE_IN_DAYS}" ]]; then
    AGE_IN_DAYS=60
    echo "AGE_IN_DAYS not set, defaulting to ${AGE_IN_DAYS}"
fi

# DELETION_WARNING_DAYS: defaults to 7 if not provided
if [[ -z "${DELETION_WARNING_DAYS}" ]]; then
    DELETION_WARNING_DAYS=7
    echo "DELETION_WARNING_DAYS not set, defaulting to ${DELETION_WARNING_DAYS}"
fi

# Validate AGE_IN_DAYS is a positive integer
if ! [[ "${AGE_IN_DAYS}" =~ ^[0-9]+$ ]] || [[ "${AGE_IN_DAYS}" -le 0 ]]; then
    echo "ERROR: AGE_IN_DAYS must be a positive integer, got: '${AGE_IN_DAYS}'"
    exit 1
fi

# Validate DELETION_WARNING_DAYS is a positive integer
if ! [[ "${DELETION_WARNING_DAYS}" =~ ^[0-9]+$ ]] || [[ "${DELETION_WARNING_DAYS}" -le 0 ]]; then
    echo "ERROR: DELETION_WARNING_DAYS must be a positive integer, got: '${DELETION_WARNING_DAYS}'"
    exit 1
fi

# Validate DELETION_WARNING_DAYS is less than AGE_IN_DAYS
if [[ "${DELETION_WARNING_DAYS}" -ge "${AGE_IN_DAYS}" ]]; then
    echo "ERROR: DELETION_WARNING_DAYS (${DELETION_WARNING_DAYS}) must be less than AGE_IN_DAYS (${AGE_IN_DAYS})"
    exit 1
fi

echo "Using retention settings: AGE_IN_DAYS=${AGE_IN_DAYS}, DELETION_WARNING_DAYS=${DELETION_WARNING_DAYS}"

# Call the script to generate the reports and upload them to Google Drive
python scripts/retirement_partner_report.py \
    --config_file=$TEMP_CONFIG_YAML \
    --google_secrets_file=$TEMP_GOOGLE_SECRETS \
    --output_dir=$PARTNER_REPORTS_DIR \
    --age_in_days=$AGE_IN_DAYS \
    --deletion_warning_days=$DELETION_WARNING_DAYS \
    --ENABLE_CHECK_EXPIRING_FILES=$ENABLE_CHECK_EXPIRING_FILES

# Remove the temporary files after processing
rm -f "$TEMP_CONFIG_YAML"
rm -f "$TEMP_GOOGLE_SECRETS"

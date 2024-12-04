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

set -x

# Prepare retirement scripts
cd $WORKSPACE/edx-platform
pip install -r scripts/user_retirement/requirements/base.txt

# Call the script to collect the list of learners that are to be retired.
python scripts/user_retirement/retirement_bulk_status_update.py \
    --config_file=$TEMP_CONFIG_YAML \
    --start_date=$START_DATE \
    --end_date=$END_DATE \
    --initial_state=$INITIAL_STATE_NAME \
    ${NEW_STATE_NAME:+ "--new_state=$NEW_STATE_NAME"} \
    $(if [[ $REWIND_STATE == "true" ]]; then echo --rewind-state; fi)

# Remove the temporary file after processing
rm -f "$TEMP_CONFIG_YAML"
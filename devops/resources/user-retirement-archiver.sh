#!/usr/bin/env bash

set +u
. /edx/var/jenkins/jobvenvs/virtualenv_tools.sh
# creates a venv with its location stored in variable "venvpath"
create_virtualenv --python=python3.8 --clear
. "$venvpath/bin/activate"
set -u

env
set -ex

cd $WORKSPACE/configuration
pip install -r util/jenkins/requirements.txt

. util/jenkins/assume-role.sh

# hide the sensitive information in the logs
set +x

CONFIG_YAML=$(aws secretsmanager get-secret-value --secret-id "${SECRET_ARN}" --region "us-east-1" --output json | jq -r '.SecretString' | yq -y .)

# Create a temporary file to store the YAML
TEMP_CONFIG_YAML=$(mktemp $WORKSPACE/tempfile.XXXXXXXXXX.yml)

# Write the YAML data to the temporary file
echo "$CONFIG_YAML" > "$TEMP_CONFIG_YAML"

set -x

assume-role ${ROLE_ARN}

# prepare edx-platform
cd $WORKSPACE/edx-platform
pip install -r scripts/user_retirement/requirements/base.txt

# In case this is being run without an explicit END_DATE, default to running with "now" - COOL_OFF_DAYS
if [[ ! -v END_DATE ]]; then
    END_DATE=$(date --iso --date "$(date --iso) - $COOL_OFF_DAYS days")
fi

# Call the script to read the retirement statuses from the LMS, send them to S3, and delete them from the LMS.
python scripts/user_retirement/retirement_archive_and_cleanup.py \
    --config_file=$TEMP_CONFIG_YAML \
    --cool_off_days=$COOL_OFF_DAYS \
    --batch_size=$BATCH_SIZE \
    --start_date=$START_DATE \
    --end_date=$END_DATE \
    --redaction_id=$BUILD_NUMBER \
    --dry_run=$DRY_RUN

# Remove the temporary file after processing
rm -f "$TEMP_CONFIG_YAML"

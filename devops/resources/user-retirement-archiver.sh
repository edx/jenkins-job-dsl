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
. util/jenkins/assume-role.sh

assume-role ${ROLE_ARN}

# prepare tubular
cd $WORKSPACE/tubular
pip install -r requirements.txt

# In case this is being run without an explicit END_DATE, default to running with "now" - COOL_OFF_DAYS
if [[ ! -v END_DATE ]]; then
    END_DATE=$(date --iso --date "$(date --iso) - $COOL_OFF_DAYS days")
fi

# Call the script to read the retirement statuses from the LMS, send them to S3, and delete them from the LMS.
python scripts/retirement_archive_and_cleanup.py \
    --config_file=$WORKSPACE/user-retirement-secure/${ENVIRONMENT_DEPLOYMENT}.yml \
    --cool_off_days=$COOL_OFF_DAYS \
    --batch_size=$BATCH_SIZE \
    --start_date=$START_DATE \
    --end_date=$END_DATE \
    --dry_run=$DRY_RUN

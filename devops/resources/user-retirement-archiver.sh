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

# Call the script to read the retirement statuses from the LMS, send them to S3, and delete them from the LMS.
python scripts/retirement_archive_and_cleanup.py \
    --config_file=$WORKSPACE/user-retirement-secure/${ENVIRONMENT_DEPLOYMENT}.yml \
    --cool_off_days=$COOL_OFF_DAYS

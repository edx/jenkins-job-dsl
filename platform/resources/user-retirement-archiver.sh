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

. util/jenkins/assume-role.sh
assume-role ${ROLE_ARN}

# prepare credentials
mkdir -p $WORKSPACE/user-retirement-secure
cp $USER_RETIREMENT_SECURE_DEFAULT $WORKSPACE/user-retirement-secure/secure-default.yml

# prepare tubular
cd $WORKSPACE/tubular
pip install -r requirements.txt

# Call the script to read the retirement statuses from the LMS, send them to S3, and delete them from the LMS.
python scripts/retirement_archive_and_cleanup.py \
    --config_file=$WORKSPACE/user-retirement-secure/$ENVIRONMENT.yml \
    --cool_off_days=$COOL_OFF_DAYS

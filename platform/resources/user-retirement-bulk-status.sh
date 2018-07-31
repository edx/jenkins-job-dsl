#!/usr/bin/env bash

set -ex

# prepare credentials
mkdir -p $WORKSPACE/user-retirement-secure
cp $USER_RETIREMENT_SECURE_DEFAULT $WORKSPACE/user-retirement-secure/secure-default.yml

# prepare tubular
cd $WORKSPACE/tubular
pip install -r requirements.txt

# Call the script to collect the list of learners that are to be retired.
python scripts/retirement_bulk_status_update.py \
    --config_file=$WORKSPACE/user-retirement-secure/$ENVIRONMENT.yml \
    --start_date=$START_DATE \
    --end_date=$END_DATE \
    --initial_state=$INITIAL_STATE_NAME \
    --new_state=$NEW_STATE_NAME

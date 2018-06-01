#!/usr/bin/env bash

set -ex

# prepare credentials
mkdir -p $WORKSPACE/user-retirement-secure
cp $USER_RETIREMENT_SECURE_DEFAULT $WORKSPACE/user-retirement-secure/secure-default.yml

# prepare tubular
cd $WORKSPACE/tubular
pip install -r requirements.txt

# Create the directory where we will populate properties files, one per
# downstream build.
rm -rf $LEARNERS_TO_RETIRE_PROPERTIES_DIR
mkdir $LEARNERS_TO_RETIRE_PROPERTIES_DIR

# Call the script to collect the list of learners that are to be retired.
python scripts/get_learners_to_retire.py \
    --config_file=$WORKSPACE/user-retirement-secure/$ENVIRONMENT.yml \
    --output_dir=$LEARNERS_TO_RETIRE_PROPERTIES_DIR \
    --cool_off_days=$COOL_OFF_DAYS \
    --user_count_error_threshold=$USER_COUNT_ERROR_THRESHOLD

#!/usr/bin/env bash

set -ex

# prepare credentials
mkdir -p $WORKSPACE/user-retirement-secure
cp $USER_RETIREMENT_SECURE_EXTRA $WORKSPACE/user-retirement-secure/secure-extra.yml

# prepare tubular
cd $WORKSPACE/tubular
pip install -r requirements.txt

# Call the script to retire one learner.  This assumes the following build
# parameters / environment variable is set: RETIREMENT_USERNAME.
python scripts/retire_one_learner.py \
    --config_file=$WORKSPACE/user-retirement-secure/$ENVIRONMENT.yml

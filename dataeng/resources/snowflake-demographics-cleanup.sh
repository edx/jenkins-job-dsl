#!/usr/bin/env bash
set -ex

# Creating Python virtual env
PYTHON_VENV="python_venv"
virtualenv --python=$PYTHON_VENV_VERSION --clear "${PYTHON_VENV}"
source "${PYTHON_VENV}/bin/activate"

# Setup
cd $WORKSPACE/analytics-tools/snowflake
make requirements


python3 secrets-manager.py -w -n analytics-secure/snowflake/rsa_key_stitch_loader.p8 -v rsa_key_stitch_loader
python3 secrets-manager.py -w -n analytics-secure/snowflake/rsa_key_passphrase_stitch_loader -v rsa_key_passphrase_stitch_loader

unset KEY_PATH
unset PASSPHRASE_PATH

python demographics_cleanup.py \
    --user "STITCH_LOADER" \
    --account "edx.us-east-1" \
    --key_file rsa_key_stitch_loader \
    --passphrase_file rsa_key_passphrase_stitch_loader


rm rsa_key_stitch_loader
rm rsa_key_passphrase_stitch_loader

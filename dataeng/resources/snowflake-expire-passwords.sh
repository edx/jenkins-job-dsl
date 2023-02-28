#!/usr/bin/env bash
set -ex

# Creating Python virtual env
PYTHON_VENV="python_venv"
virtualenv --python=$PYTHON_VENV_VERSION --clear "${PYTHON_VENV}"
source "${PYTHON_VENV}/bin/activate"

# Setup
cd $WORKSPACE/analytics-tools/snowflake
make requirements

python expire_user_passwords.py \
    --key_path $KEY_PATH \
    --passphrase_path $PASSPHRASE_PATH \
    --automation_user $USER \
    --account $ACCOUNT

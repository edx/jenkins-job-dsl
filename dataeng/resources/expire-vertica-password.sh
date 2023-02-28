#!/usr/bin/env bash
set -ex

# Creating Python virtual env
PYTHON_VENV="python_venv"
virtualenv --python=$PYTHON_VENV_VERSION --clear "${PYTHON_VENV}"
source "${PYTHON_VENV}/bin/activate"

# Setup
cd $WORKSPACE/analytics-tools/vertica
pip install -r requirements.txt

python expire_user_passwords.py \
    --credentials $CREDENTIALS \
    --exclude $EXCLUDE \
    --mapping $MAPPING

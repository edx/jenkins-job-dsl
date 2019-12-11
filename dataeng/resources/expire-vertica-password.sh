#!/usr/bin/env bash
set -ex

# Setup
cd $WORKSPACE/analytics-tools/vertica
pip install -r requirements.txt

python expire_user_passwords.py \
    --credentials $CREDENTIALS \
    --exclude $EXCLUDE \
    --mapping $MAPPING

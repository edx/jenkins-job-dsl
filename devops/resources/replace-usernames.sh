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

# prepare tubular
cd $WORKSPACE/tubular
pip install -r requirements.txt

# Call the script to replace the usernames for all users in the CSV file.
python scripts/replace_usernames.py \
    --config_file=$USERNAME_REPLACEMENT_CONFIG_FILE
    --username_replacement_csv=$WORKSPACE/username_replacements.csv

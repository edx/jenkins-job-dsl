#!/usr/bin/env bash
set -ex


# Creating Python virtual env
PYTHON_VENV="python_venv"
virtualenv --python=$PYTHON_VENV_VERSION --clear "${PYTHON_VENV}"
source "${PYTHON_VENV}/bin/activate"

# Setup
cd $WORKSPACE/analytics-tools/snowflake
make requirements

# Define the location of the script in the Jenkins workspace
SCRIPT_PATH="$WORKSPACE/secrets-manager.sh"


set +x

echo "Running secrets manager script"
python3 secrets-manager.py -w -n analytics-secure/snowflake/rsa_key_snowpipe_user.p8 -v rsa_key_snowpipe_user
python3 secrets-manager.py -w -n analytics-secure/snowflake/rsa_key_passphrase_snowpipe_user -v rsa_key_passphrase_snowpipe_user
#set -x

unset KEY_PATH
unset PASSPHRASE_PATH

python refresh_snowpipe.py \
    --user 'SNOWPIPE' \
    --schema $SCHEMA \
    --account 'edx.us-east-1' \
    --pipe_name $PIPE_NAME \
    --table_name $TABLE_NAME \
    --delay $DELAY \
    --limit $LIMIT \
    --key_file "$(cat "rsa_key_snowpipe_user")" \
    --passphrase_file "$(cat "rsa_key_passphrase_snowpipe_user")"

rm rsa_key_snowpipe_user
rm rsa_key_passphrase_snowpipe_user


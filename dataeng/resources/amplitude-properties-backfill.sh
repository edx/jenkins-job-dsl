#!/usr/bin/env bash
set -ex

# Creating Python virtual env
PYTHON_VENV="python_venv"
virtualenv --python=$PYTHON_VENV_VERSION --clear "${PYTHON_VENV}"
source "${PYTHON_VENV}/bin/activate"

# Setup
cd $WORKSPACE/analytics-tools/snowflake
make requirements

# Do not print commands in this function since they may contain secrets.
set +x

# Retrieve a vault token corresponding to the jenkins AppRole.  The token is then stored in the VAULT_TOKEN variable
# which is implicitly used by subsequent vault commands within this script.
# Instructions followed: https://learn.hashicorp.com/tutorials/vault/approle#step-4-login-with-roleid-secretid
export VAULT_TOKEN=$(vault write -field=token auth/approle/login \
      role_id=${ANALYTICS_VAULT_ROLE_ID} \
      secret_id=${ANALYTICS_VAULT_SECRET_ID}
  )

API_KEY=$(
  vault kv get \
    -version=${AMPLITUDE_VAULT_KV_VERSION} \
    -field=API_KEY \
    ${AMPLITUDE_VAULT_KV_PATH} \


python3 secrets-manager.py -w -n analytics-secure/snowflake/rsa_key_snowpipe_user.p8 -v rsa_key_snowflake_task_automation_user
python3 secrets-manager.py -w -n analytics-secure/snowflake/rsa_key_passphrase_snowpipe_user -v rsa_key_passphrase_snowflake_task_automation_user


python amplitude_user_properties_update.py \
    --automation_user 'SNOWFLAKE_TASK_AUTOMATION_USER' \
    --account 'edx.us-east-1' \
    --amplitude_data_source_table $AMPLITUDE_DATA_SOURCE_TABLE \
    --columns_to_update $COLUMNS_TO_UPDATE \
    --response_table $RESPONSE_TABLE \
    --amplitude_operation_name $AMPLITUDE_OPERATION_NAME \
    --amplitude_api_key $API_KEY \
    --key_file rsa_key_snowflake_task_automation_user \
    --passphrase_file rsa_key_passphrase_snowflake_task_automation_user

rm rsa_key_snowflake_task_automation_user
rm rsa_key_passphrase_snowflake_task_automation_user

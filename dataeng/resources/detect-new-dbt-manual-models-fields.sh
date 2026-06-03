#!/usr/bin/env bash
set -ex

# Global variables
DBT_PROFILE_ARGS="--profiles-dir ${WORKSPACE}/analytics-secure/warehouse-transforms/ --profile ${DBT_PROFILE} --target ${DBT_TARGET}"
DBT_TARGET_PATH=${WORKSPACE}/warehouse-transforms/projects/${DBT_PROJECT_PATH}/target

# Set up a virtual environment for warehouse-transforms
PYTHON38_VENV="py38_venv_warehouse_transforms"
virtualenv --python=python3.8 --clear "${PYTHON38_VENV}"
source "${PYTHON38_VENV}/bin/activate"
pip install -U pip
pip install -r ${WORKSPACE}/warehouse-transforms/requirements.txt

# Go into the automated/applications project and compile the manual models
cd ${WORKSPACE}/warehouse-transforms/projects/${DBT_PROJECT_PATH}
dbt clean ${DBT_PROFILE_ARGS}
dbt deps ${DBT_PROFILE_ARGS}
dbt compile ${DBT_PROFILE_ARGS} --select tag:manual

# Deactivate the virtual environment, so we can create and activate another one
deactivate

# Setup a virtual environment for analytics-tools
PYTHON38_VENV="py38_venv_analytics_tools"
virtualenv --python=python3.8 --clear "${PYTHON38_VENV}"
source "${PYTHON38_VENV}/bin/activate"

# Go into analytics-tools and install the dependencies
cd ${WORKSPACE}/analytics-tools/snowflake
make requirements

# Create a function to clean up the credential files, and trap EXIT with it
function clean_up_files() {
  rm -rf .snowflake_private_key .snowflake_private_key_passphrase
}
trap clean_up_files EXIT

# Fetch credentials from vault
# Do not print commands in this function since they may contain secrets.
set +x

# Retrieve a vault token corresponding to the jenkins AppRole.  The token is then stored in the VAULT_TOKEN variable
# which is implicitly used by subsequent vault commands within this script.
# Instructions followed: https://learn.hashicorp.com/tutorials/vault/approle#step-4-login-with-roleid-secretid
export VAULT_TOKEN=$(vault write -field=token auth/approle/login \
    role_id=${ANALYTICS_VAULT_ROLE_ID} \
    secret_id=${ANALYTICS_VAULT_SECRET_ID}
)

set -x

# JIRA webhook URL and secret string from vault
JIRA_WEBHOOK_URL=$(
  vault kv get \
    -version=${JIRA_WEBHOOK_VAULT_KV_VERSION} \
    -field=JIRA_WEBHOOK_URL \
    ${JIRA_WEBHOOK_VAULT_KV_PATH} \
)
JIRA_WEBHOOK_SECRET=$(
  vault kv get \
    -version=${JIRA_WEBHOOK_VAULT_KV_VERSION} \
    -field=JIRA_WEBHOOK_SECRET \
    ${JIRA_WEBHOOK_VAULT_KV_PATH} \
)

# Snowflake credentials from vault
SNOWFLAKE_ACCOUNT=$(
  vault kv get \
    -version=${AUTOMATION_TASK_USER_VAULT_KV_VERSION} \
    -field=account \
    ${AUTOMATION_TASK_USER_VAULT_KV_PATH} \
)

SNOWFLAKE_USER=$(
  vault kv get \
    -version=${AUTOMATION_TASK_USER_VAULT_KV_VERSION} \
    -field=user \
    ${AUTOMATION_TASK_USER_VAULT_KV_PATH} \
)
# The detect_new_raw_columns.py script, much like all other scripts that connect to Snowflake,
# expects the private key and the privarte key passphrase to be in files.
# As a result, SNOWFLAKE_PRIVATE_KEY and SNOWFLAKE_PRIVATE_KEY_PASSPHRASE are stored in files.
vault kv get \
    -version=${AUTOMATION_TASK_USER_VAULT_KV_VERSION} \
    -field=private_key \
    ${AUTOMATION_TASK_USER_VAULT_KV_PATH} > .snowflake_private_key

vault kv get \
    -version=${AUTOMATION_TASK_USER_VAULT_KV_VERSION} \
    -field=private_key_passphrase \
    ${AUTOMATION_TASK_USER_VAULT_KV_PATH} > .snowflake_private_key_passphrase

# Invoke the script to detect new fields that need to be added manually
python detect_new_raw_columns.py ${DBT_TARGET_PATH} \
    --user ${SNOWFLAKE_USER} --account ${SNOWFLAKE_ACCOUNT} \
    --key-path .snowflake_private_key --passphrase-path .snowflake_private_key_passphrase \
    --jira-webhook-url ${JIRA_WEBHOOK_URL} \
    --jira-webhook-secret ${JIRA_WEBHOOK_SECRET}

#!/usr/bin/env bash
#
# Fetch the latest studio and LMS configs for edx and edge.
#
# For each deployment:
#
# 1. Assume the cross-account role specifically made for this purpose.
# 2. Fetch the encrypted configs.
# 3. Login to Vault.
# 4. Fetch the decryption keys from Vault.
# 5. Decrypt the configs

set -xe

env | sort

# Install dependencies early in this script.
pip install asym-crypto-yaml

assume_role () {
    # Assume an AWS role.
    #
    # Args:
    #   - str: The role ARN to assume.

    # Do not print commands in this function since they may contain secrets.
    set +x

    ROLE_ARN=$1
    AWS_SESSION_NAME="$(date +"%s")_${USER}@$(hostname)"
    RESULT=$(aws sts assume-role --role-arn "${ROLE_ARN}" --role-session-name ${AWS_SESSION_NAME:0:32})
    export AWS_ACCESS_KEY_ID=$(echo $RESULT | jq --raw-output .Credentials.AccessKeyId)
    export AWS_SECRET_ACCESS_KEY=$(echo $RESULT | jq --raw-output .Credentials.SecretAccessKey)
    export AWS_SESSION_TOKEN=$(echo $RESULT | jq --raw-output .Credentials.SessionToken)
    echo "Successfully assumed role ${ROLE_ARN}" >&2

    # Re-enable printing of commands.
    set -x
}
unassume_role () {
    # Revert back to the instance default role.
    unset AWS_ACCESS_KEY_ID
    unset AWS_SECRET_ACCESS_KEY
    unset AWS_SESSION_TOKEN
}

# Step 1: fetch the encrypted configs.
#
# For this to work, the job config needs to following variables for both edx and edge:
#
# * REMOTE_CONFIG_PROD_EDX_ROLE_ARN  - The role in the production account that we will assume.
# * REMOTE_CONFIG_PROD_EDX_LMS       - The S3 path to the LMS remote configs.
# * REMOTE_CONFIG_PROD_EDX_STUDIO    - The S3 path to the STUDIO remote configs.
# * REMOTE_CONFIG_PROD_EDGE_ROLE_ARN
# * REMOTE_CONFIG_PROD_EDGE_LMS
# * REMOTE_CONFIG_PROD_EDGE_STUDIO
#
# And they must be loaded via the job DSL like so:
#
# environmentVariables {
#     env('REMOTE_CONFIG_PROD_EDX_ROLE_ARN', allVars.get('REMOTE_CONFIG_PROD_EDX_ROLE_ARN'))
#     env('REMOTE_CONFIG_PROD_EDX_LMS', allVars.get('REMOTE_CONFIG_PROD_EDX_LMS'))
#     env('REMOTE_CONFIG_PROD_EDX_STUDIO', allVars.get('REMOTE_CONFIG_PROD_EDX_STUDIO'))
#     env('REMOTE_CONFIG_PROD_EDGE_ROLE_ARN', allVars.get('REMOTE_CONFIG_PROD_EDGE_ROLE_ARN'))
#     env('REMOTE_CONFIG_PROD_EDGE_LMS', allVars.get('REMOTE_CONFIG_PROD_EDGE_LMS'))
#     env('REMOTE_CONFIG_PROD_EDGE_STUDIO', allVars.get('REMOTE_CONFIG_PROD_EDGE_STUDIO'))
#     env('REMOTE_CONFIG_DECRYPTION_KEYS_VAULT_KV_PATH', allVars.get('REMOTE_CONFIG_DECRYPTION_KEYS_VAULT_KV_PATH'))
#     env('REMOTE_CONFIG_DECRYPTION_KEYS_VAULT_KV_VERSION', allVars.get('REMOTE_CONFIG_DECRYPTION_KEYS_VAULT_KV_VERSION'))
# }

# Fetch the prod-edx encrypted remote-configs.
mkdir -p ${WORKSPACE}/remote-config/prod-edx/
assume_role ${REMOTE_CONFIG_PROD_EDX_ROLE_ARN}
aws s3 cp ${REMOTE_CONFIG_PROD_EDX_LMS}    ${WORKSPACE}/remote-config/prod-edx/lms.encrypted.yml
aws s3 cp ${REMOTE_CONFIG_PROD_EDX_STUDIO} ${WORKSPACE}/remote-config/prod-edx/studio.encrypted.yml
unassume_role

# Fetch the prod-edge encrypted remote-configs.
mkdir -p ${WORKSPACE}/remote-config/prod-edge/
assume_role ${REMOTE_CONFIG_PROD_EDGE_ROLE_ARN}
aws s3 cp ${REMOTE_CONFIG_PROD_EDGE_LMS}    ${WORKSPACE}/remote-config/prod-edge/lms.encrypted.yml
aws s3 cp ${REMOTE_CONFIG_PROD_EDGE_STUDIO} ${WORKSPACE}/remote-config/prod-edge/studio.encrypted.yml
unassume_role

# Login to Vault.
#
# For this to work, any job that uses this script needs the following section:
#
# wrappers {
#     credentialsBinding {
#         usernamePassword('ANALYTICS_VAULT_ROLE_ID', 'ANALYTICS_VAULT_SECRET_ID', 'analytics-vault');
#     }
# }
#
# Retrieve a vault token corresponding to the jenkins AppRole.  The token is then stored in the VAULT_TOKEN variable
# which is implicitly used by subsequent vault commands within this script.
# Instructions followed: https://learn.hashicorp.com/tutorials/vault/approle#step-4-login-with-roleid-secretid
VAULT_TOKEN=$(vault write -field=token auth/approle/login \
    role_id=${ANALYTICS_VAULT_ROLE_ID} \
    secret_id=${ANALYTICS_VAULT_SECRET_ID}
)

# For each deployment, fetch the appropriate decryption keys from Vault and decrypt lms and studio configs.
for DEPLOYMENT in edx edge; do
    DECRYPTION_KEY_PATH=${WORKSPACE}/remote-config/config-decryption-key
    # First, fetch the decryption key for the given deployment.
    #
    # FYI: in bash, double carets after a variable name capitalizes the string.
    vault kv get \
        -version=${REMOTE_CONFIG_DECRYPTION_KEYS_VAULT_KV_VERSION} \
        -field=PROD_${DEPLOYMENT^^}_PRIVATE_KEY \
        ${REMOTE_CONFIG_DECRYPTION_KEYS_VAULT_KV_PATH} \
        >${DECRYPTION_KEY_PATH}

    # Now that we have the decryption key, decrypt lms and studio configs:
    asym_crypto_yaml decrypt-encrypted-yaml \
        --private_key_path ${DECRYPTION_KEY_PATH} \
        --secrets_file_path ${WORKSPACE}/remote-config/prod-${DEPLOYMENT}/lms.encrypted.yml \
        --outfile_path ${WORKSPACE}/remote-config/prod-${DEPLOYMENT}/lms.yml
    asym_crypto_yaml decrypt-encrypted-yaml \
        --private_key_path ${DECRYPTION_KEY_PATH} \
        --secrets_file_path ${WORKSPACE}/remote-config/prod-${DEPLOYMENT}/studio.encrypted.yml \
        --outfile_path ${WORKSPACE}/remote-config/prod-${DEPLOYMENT}/studio.yml

    # For good measure, delete the decryption key.  The wsCleanup() post-build step would delete anyway if this fails
    # for some reason.
    rm ${DECRYPTION_KEY_PATH}
done

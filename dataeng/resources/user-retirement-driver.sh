#!/usr/bin/env bash

set -ex

# Display the environment variables again, this time within the context of a
# new subshell inside of the job. N.B. this should not print plain credentials
# because the only credentialsBindings we currently use is of type "file" which
# just stores a filename in the environment (rather than the content).
env

# Create and activate a virtualenv.  In case we ever change the concurrency
# setting on the jenkins worker, it would be safest to keep the builds from
# clobbering each other's virtualenvs.
VENV="venv-${BUILD_NUMBER}"
# Python 3.12 is not packaged for the Ubuntu 20.04 Jenkins workers (deadsnakes stopped at
# Focal). Use the system python3.12 when the host has one; otherwise fetch a standalone build
# with uv into the jenkins user's home (~/.local/share/uv, cached across runs). No root needed.
if command -v python3.12 >/dev/null 2>&1; then
    PYTHON_312="$(command -v python3.12)"
else
    python3 -m pip install --user --quiet "uv==0.12.20"
    export PATH="${HOME}/.local/bin:${PATH}"
    uv python install 3.12
    PYTHON_312="$(uv python find 3.12)"
fi
# stdlib venv: the host virtualenv (20.2.0 on py3.8) cannot seed a 3.12 environment.
"${PYTHON_312}" -m venv --clear "${VENV}"
source "${VENV}/bin/activate"

# Make sure that when we try to write unicode to the console, it
# correctly encodes to UTF-8 rather than exiting with a UnicodeEncode
# error.
export PYTHONIOENCODING=UTF-8
export LC_CTYPE=en_US.UTF-8

#Fetch secrets from AWS
cd $WORKSPACE/configuration
pip install -r util/jenkins/requirements.txt
# hide the sensitive information in the logs
set +x

CONFIG_YAML=$(aws secretsmanager get-secret-value --secret-id "user-retirement-secure/$ENVIRONMENT" --region "us-east-1" --output json | jq -r '.SecretString' | yq -y .)

# Create a temporary file to store the YAML
TEMP_CONFIG_YAML=$(mktemp $WORKSPACE/tempfile.XXXXXXXXXX.yml)

# Write the YAML data to the temporary file
echo "$CONFIG_YAML" > "$TEMP_CONFIG_YAML"

set -x

# prepare tubular
cd $WORKSPACE/tubular
# pip 21 cannot run on Python 3.12; use current pip/setuptools.
pip install --upgrade pip "setuptools>=68"
pip install -r requirements.txt

# Call the script to retire one learner.  This assumes the following build
# parameters / environment variable is set: RETIREMENT_USERNAME.
python scripts/retire_one_learner.py \
    --config_file=$TEMP_CONFIG_YAML

# Remove the temporary file after processing
rm -f "$TEMP_CONFIG_YAML"

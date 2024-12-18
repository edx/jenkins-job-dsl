#!/usr/bin/env bash
set -e

# Create destination directory
mkdir -p /var/lib/jenkins/tmp/analytics-exporter/course-data

# Create and activate a virtualenv in shell script
EXPORTER_VENV="exporter_venv"
virtualenv --python=python3.11 --clear "${EXPORTER_VENV}"
source "${EXPORTER_VENV}/bin/activate"

cd $WORKSPACE/analytics-tools/snowflake
pip install boto3

python3 secrets-manager.py -w -n analytics-secure/analytics-exporter/task-auth.json -v ${WORKSPACE}/analytics-secure/analytics-exporter/task-auth.json
cd $WORKSPACE

# Install requirements into this (exporter) virtual environment
pushd analytics-exporter/
pip install 'setuptools<65'
pip install -r github_requirements.txt
pip install mysql-connector-python -e .
popd

# Configuration paths in analytics-secure
SECURE_ROOT=${WORKSPACE}/analytics-secure/analytics-exporter
CONFIG_PATH=${SECURE_ROOT}/${EXPORTER_CONFIG_FILENAME}
GPG_KEYS_PATH=${WORKSPACE}/data-czar-keys

# Save virtualenv location and configuration paths
echo "
EXPORTER_VENV=${WORKSPACE}/${EXPORTER_VENV}
CONFIG_PATH=${CONFIG_PATH}
GPG_KEYS_PATH=${GPG_KEYS_PATH}
DATE=$(date +%d ${DATE_MODIFIER})
ORG_CONFIG_PATH=${WORKSPACE}/${ORG_CONFIG}
" > exporter_vars

env | sort

# Export job configuration files.
# 'orgranizations' is the directory where to save Jenkins property files and is used by fileBuildParameterFactory in
# master job DSL to generate parameters for each of the worker jobs.
exporter-properties \
    --output-bucket=${OUTPUT_BUCKET} \
    --output-prefix=${OUTPUT_PREFIX} \
    --orgs="${ORGS}" \
    --include=platform_venv_path \
    --include=exporter_vars \
    ${CONFIG_PATH} \
    ${WORKSPACE}/${ORG_CONFIG} \
    organizations

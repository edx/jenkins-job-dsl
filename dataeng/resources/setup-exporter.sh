#!/usr/bin/env bash
set -e

# Create destination directory
mkdir -p /var/lib/jenkins/tmp/analytics-exporter/course-data

# Create and activate a virtualenv in shell script
EXPORTER_VENV="exporter_venv"
virtualenv --python=python3.11 --clear "${EXPORTER_VENV}"
source "${EXPORTER_VENV}/bin/activate"

# Install requirements into this (exporter) virtual environment
pushd analytics-exporter/
pip install 'setuptools<65'
pip install -r github_requirements.txt
pip install mysql-connector-python -e .
popd



cd analytics-tools/snowflake
pip install argparse boto3 
python3 secrets-manager.py -w -n analytics-secure/analytics-exporter/task-auth.json -v task-auth.json
cd ../../

# Configuration paths in analytics-config
SECURE_ROOT=${WORKSPACE}/analytics-config/analytics-exporter
CONFIG_PATH=${SECURE_ROOT}/${EXPORTER_CONFIG_FILENAME}
GPG_KEYS_PATH=${WORKSPACE}/data-czar-keys

# Save virtualenv location and configuration paths
echo "
EXPORTER_VENV=${WORKSPACE}/${EXPORTER_VENV}
CONFIG_PATH=${CONFIG_PATH}
GPG_KEYS_PATH=${GPG_KEYS_PATH}
DATE=$(date +%d ${DATE_MODIFIER})
EXTRA_OPTIONS=${EXTRA_OPTIONS}
ORG_CONFIG_PATH=${WORKSPACE}/${ORG_CONFIG}
SECURE_BRANCH=${SECURE_BRANCH}
" > exporter_vars

env | sort


# Export job configuration files
exporter-properties \
    --output-bucket=${OUTPUT_BUCKET} \
    --orgs="${ORGS}" \
    --include=platform_venv_path \
    --include=exporter_vars \
    ${CONFIG_PATH} \
    ${WORKSPACE}/${ORG_CONFIG} \
    organizations

# Dirty hack:
# Some orgs can take an exceptionally long time to run. Depending on the concurrency
# settings for the analytics-exporter-master job and the location of the organization
# alphabetically in the organizations directory, it's possible that these long-running
# jobs will be started towards the end of the master run, which can extend the total
# run-time quite a bit. Use PRIORITY_ORGS to pass a space separated list of orgs that
# should run first. This is accomplished by prepending a number to the name of the orgs
# in question.
for ORG in ${PRIORITY_ORGS}; do
    if [ -f organizations/$ORG ]; then
        mv organizations/$ORG organizations/1_$ORG
    fi
done

#!/usr/bin/env bash
set -e

# Create destination directory
mkdir -p /var/lib/jenkins/tmp/analytics-exporter/course-data

# Create and activate a virtualenv in shell script
EXPORTER_VENV="exporter_venv"
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
"${PYTHON_312}" -m venv --clear "${EXPORTER_VENV}"
source "${EXPORTER_VENV}/bin/activate"

cd $WORKSPACE/analytics-tools/snowflake
pip install boto3

python3 secrets-manager.py -w -n analytics-secure/analytics-exporter/task-auth.json -v ${WORKSPACE}/analytics-secure/analytics-exporter/task-auth.json
cd $WORKSPACE

# Install the exporter into this virtual environment.
# requirements.txt is read by pbr as install_requires (it now includes
# mysql-connector-python from PyPI; github_requirements.txt no longer exists).
pushd analytics-exporter/
pip install --upgrade pip "setuptools>=68"
pip install -e .
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

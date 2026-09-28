#!/usr/bin/env bash
set -e

# Create the exporter virtual env (edx-analytics-exporter requires Python >= 3.12)
PYTHON_VENV="python_venv"
virtualenv --python=python3.12 --clear "${PYTHON_VENV}"
source "${PYTHON_VENV}/bin/activate"

# Create destination directory
WORKING_DIRECTORY=/var/lib/jenkins/tmp/analytics-course-exporter
mkdir -p ${WORKING_DIRECTORY}/course-data

# Install the exporter into this virtual environment.
# requirements.txt is read by pbr as install_requires (it now includes
# mysql-connector-python from PyPI; github_requirements.txt no longer exists).
pushd analytics-exporter/
pip install --upgrade pip "setuptools>=68"
pip install -e .
popd

# Get name of other (platform) virtual environment
source platform_venv_path

# Configuration paths in analytics-secure
SECURE_ROOT=${WORKSPACE}/analytics-secure/analytics-exporter
CONFIG_PATH=${SECURE_ROOT}/${EXPORTER_CONFIG_FILENAME}

DATE=$(date +%d ${DATE_MODIFIER})
TODAY=$(date +%d)

env | sort

# Export job configuration files
course-exporter \
   ${COURSES} \
   ${TASKS} \
   --work-dir=${WORKING_DIRECTORY} \
   --output-bucket=${OUTPUT_BUCKET} \
   --external-prefix=databases/${DATE:-$TODAY} \
   --django-admin=${PLATFORM_VENV}/bin/django-admin.py \
   --django-pythonpath=${PLATFORM_VENV}/edx-platform \
   ${CONFIG_PATH}

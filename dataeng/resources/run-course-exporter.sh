#!/usr/bin/env bash
set -e

# Creating python2.7 virtual env
PYTHON_VENV="python_venv"
virtualenv --python=python2.7 --clear "${PYTHON_VENV}"
source "${PYTHON_VENV}/bin/activate"

# Create destination directory
WORKING_DIRECTORY=/var/lib/jenkins/tmp/analytics-course-exporter
mkdir -p ${WORKING_DIRECTORY}/course-data

# Install requirements into this (exporter) virtual environment
pushd analytics-exporter/
pip install 'setuptools<45'
pip install -r github_requirements.txt
pip install mysql-connector-python -e .
popd

# Get name of other (platform) virtual environment
source platform_venv_path

# Configuration paths in analytics-secure
SECURE_ROOT=${WORKSPACE}/dataeng/analytics-exporter
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

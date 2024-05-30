#!/usr/bin/env bash
#
# This version of setup-platform-venv is specifically designed for newer
# python3 versions of the platform.


# Create and activate a virtualenv in shell script
PLATFORM_VENV="platform_venv"
virtualenv --python=python3.8 --clear "${PLATFORM_VENV}"
source "${PLATFORM_VENV}/bin/activate"

# Install requirements
pushd edx-platform
# This is the same pip version we currently pin in our devstack edxapp container:
pip install pip==20.2.3
make requirements
# This is a new requirement for the skill-tagging plugin, specifically for 2U
pip install skill-tagging==0.2.0

popd

# Save virtualenv location
echo "PLATFORM_VENV=${WORKSPACE}/${PLATFORM_VENV}" > platform_venv_path

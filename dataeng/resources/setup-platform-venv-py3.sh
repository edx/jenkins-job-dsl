#!/usr/bin/env bash
#
# This version of setup-platform-venv is specifically designed for newer
# python3 versions of the platform.

# Install requirements
pushd edx-platform
# This is the same pip version we currently pin in our devstack edxapp container:
pip install pip==20.0.2
make requirements
popd

# Save virtualenv location
echo "PLATFORM_VENV=${VIRTUAL_ENV}" > platform_venv

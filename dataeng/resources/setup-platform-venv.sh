#!/usr/bin/env bash

# Install requirements
pushd edx-platform
make requirements
popd

# Save virtualenv location
echo "PLATFORM_VENV=${VIRTUAL_ENV}" > platform_venv

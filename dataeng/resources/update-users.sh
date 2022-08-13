#!/usr/bin/env bash
set -ex

# Creating python 3.8 virtual environment
PYTHON27_VENV="py27_venv"
virtualenv --python=python2.7 --clear "${PYTHON27_VENV}"
source "${PYTHON27_VENV}/bin/activate"

# Change into the analytics-configuration folder and call make
cd analytics-configuration && make users.update

#!/usr/bin/env bash
set -e

virtualenv --python=python$PYTHON_VERSION venv
. venv/bin/activate

echo "Installing python requirements..."
pip install -q -r requirements/testing.txt

python -m travis.build_info --org edx --log-level debug

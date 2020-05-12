#!/usr/bin/env bash
set -e

virtualenv --python=python$PYTHON_VERSION venv
. venv/bin/activate

echo "Installing requirements..."
pip install -q -r requirements/testing.txt

python -m travis.build_info --org edx --task-class job --log-level debug

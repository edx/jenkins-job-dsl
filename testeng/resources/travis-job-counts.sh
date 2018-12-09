#!/usr/bin/env bash
set -e

virtualenv venv
. venv/bin/activate

echo "Installing requirements..."
pip install -q -r test-requirements.txt

python -m travis.build_info --org edx --task-class job --log-level debug

#!/bin/bash
set -e

virtualenv --python=python3.8 test_notifier_venv -q
source test_notifier_venv/bin/activate

echo "Installing python requirements..."
pip install -q -r requirements/base.txt

echo "Running test notifier script..."
python jenkins/codecov_response_metrics.py

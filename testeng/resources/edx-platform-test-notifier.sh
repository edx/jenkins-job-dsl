#!/bin/bash
set -e

virtualenv --python=python3.8 test_notifier_venv -q
source test_notifier_venv/bin/activate

echo "Installing python requirements..."
pip install -q -r requirements/base.txt

echo "Running test notifier script..."
python -m jenkins.edx_platform_test_notifier --repo $REPO --pr_number $PR_NUMBER

#!/bin/bash
set -e

virtualenv test_notifier_venv -q
source test_notifier_venv/bin/activate

echo "Installing python requirements..."
pip install -q -r requirements.txt

echo "Running test notifier script..."
python jenkins/edx-platform-test-notifier.py --pr_number $PR_NUMBER

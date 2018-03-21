#!/bin/bash
set -e

virtualenv trigger_firefox_venv -q
source trigger_firefox_venv/bin/activate

echo "Installing python requirements..."
pip install -q -r requirements.txt

echo "Running test notifier script..."
python jenkins/trigger-firefox-jobs.py

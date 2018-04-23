#!/bin/bash
set -e

virtualenv workers_venv -q
source workers_venv/bin/activate

echo "Installing python requirements..."
pip install -q -r requirements/base.txt

echo "Counting workers for build jenkins..."
python jenkins/workers.py -j https://build.testeng.edx.org

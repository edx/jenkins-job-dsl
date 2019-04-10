#!/bin/bash
set -e

virtualenv upgrade_venv -q
source upgrade_venv/bin/activate

echo "Creating branch..."
cd $REPO_NAME
export CURRENT_SHA=$(git rev-parse HEAD)

echo "Running make upgrade..."
make upgrade

echo "Running script to create PR..."
cd ../testeng-ci
pip install -r requirements/base.txt
cd jenkins
python upgrade_python_requirements.py --sha $CURRENT_SHA --repo_root "../../$REPO_NAME" --org $ORG

deactivate
rm -rf upgrade_venv

#!/bin/bash
set -e

rm -rf upgrade_venv
virtualenv --python=python$PYTHON_VERSION upgrade_venv -q
source upgrade_venv/bin/activate

echo "Upgrading pip..."
pip install pip==20.0.2

echo "Running make upgrade..."
cd $REPO_NAME
make upgrade

echo "Running script to create PR..."
cd ../testeng-ci
pip install -r requirements/base.txt
python -m jenkins.upgrade_python_requirements --repo_root="../$REPO_NAME" --user_reviewers=$PR_USER_REVIEWERS --team_reviewers=$PR_TEAM_REVIEWERS

deactivate
cd ..
rm -rf upgrade_venv

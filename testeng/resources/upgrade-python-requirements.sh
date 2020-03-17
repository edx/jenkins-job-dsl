#!/bin/bash
set -e

if [ -z "$SYSTEM_REQUIREMENTS" ]
then
    echo "No system requirements to install."
else
    echo "Installing system requirements..."
    apt-get install $SYSTEM_REQUIREMENTS
fi

rm -rf upgrade_venv
virtualenv --python=python$PYTHON_VERSION upgrade_venv -q
source upgrade_venv/bin/activate

echo "Getting current sha..."
cd $REPO_NAME
export CURRENT_SHA=$(git rev-parse HEAD)

echo "Running make upgrade..."
make upgrade

echo "Running script to create PR..."
cd ../testeng-ci
pip install -r requirements/base.txt
python -m jenkins.upgrade_python_requirements --sha=$CURRENT_SHA --repo_root="../$REPO_NAME" --org=$ORG --user_reviewers=$PR_USER_REVIEWERS --team_reviewers=$PR_TEAM_REVIEWERS

deactivate
cd ..
rm -rf upgrade_venv

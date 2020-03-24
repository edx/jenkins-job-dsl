#!/bin/bash
set -e -v

# click requires this to work cause it interfaces weirdly with python 3 ASCII default
export LC_ALL=C.UTF-8
export LANG=C.UTF-8

virtualenv --python=/usr/bin/python3.6 venv -q
source venv/bin/activate

# get name of target_repo
cd target_repo
REPO_NAME="$(basename -s .git `git config --get remote.origin.url`)"
OUTPUT_FILE_POSTFIX="_repo_health.yaml"
OUTPUT_FILE_NAME=$REPO_NAME$OUTPUT_FILE_POSTFIX

# install pytest and all necessary things
cd ../pytest-repo-health
make requirements
pip install -e .
deactivate
source ../venv/bin/activate

# (TODO: jinder):I need to add install for checks as well


# Run checks
touch tmp.txt
pytest --repo-health --repo-health-path ../edx-repo-health --repo-path ../target_repo --output-path $OUTPUT_FILE_NAME -c tmp.txt --noconftest
mv $OUTPUT_FILE_NAME ../data_repo/$OUTPUT_FILE_NAME

cd ../data_repo
export CURRENT_SHA=$(git rev-parse HEAD)

echo "Running script to create PR..."
cd ../testeng-ci
pip install -r requirements/base.txt
python -m jenkins.upgrade_python_requirements --sha=$CURRENT_SHA --repo_root="../data_repo" --repo_name=$REPO_NAME --org='edx' --user_reviewers=$PR_USER_REVIEWERS --team_reviewers=$PR_TEAM_REVIEWERS

# Remove all downloaded things
deactivate
cd ..
rm -rf venv

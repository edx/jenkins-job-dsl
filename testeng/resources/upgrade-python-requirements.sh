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

pr_body="$(cat <<EOF
Python requirements update.  Please review the [changelogs](\
https://openedx.atlassian.net/wiki/spaces/TE/pages/1001521320/Python+Package+Changelogs\
) for the upgraded packages.
EOF
)"

python -m jenkins.pull_request_creator --repo-root="../$REPO_NAME" \
       --base-branch-name="upgrade-python-requirements" --commit-message="Updating Python Requirements" \
       --pr-title="Python Requirements Update" --pr-body="$pr_body" \
       --user-reviewers="$PR_USER_REVIEWERS" --team-reviewers="$PR_TEAM_REVIEWERS" \
       --delete-old-pull-requests


deactivate
cd ..
rm -rf upgrade_venv

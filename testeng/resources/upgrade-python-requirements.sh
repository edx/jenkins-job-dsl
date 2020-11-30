#!/bin/bash
set -eu -o pipefail

repo_dir="$WORKSPACE/repo_to_upgrade"
venv="$WORKSPACE/upgrade_venv"

virtualenv --python="python$PYTHON_VERSION" "$venv" --clear --quiet
source "$venv/bin/activate"

echo "Upgrading pip..."
pip install "pip==20.0.2"

echo "Running make upgrade..."
cd "$repo_dir"
make upgrade

echo "Running script to create PR..."
cd "$WORKSPACE/testeng-ci"
pip install -r requirements/base.txt

pr_body="$(cat <<EOF
Python requirements update.  Please review the [changelogs](\
https://openedx.atlassian.net/wiki/spaces/TE/pages/1001521320/Python+Package+Changelogs\
) for the upgraded packages.
EOF
)"

# Note that Jenkins omits any environment variables that are empty or
# whitespace, so we need to default the reviewer vars to empty if
# they're missing.
python -m jenkins.pull_request_creator --repo-root="$repo_dir" \
       --base-branch-name="upgrade-python-requirements" --commit-message="Updating Python Requirements" \
       --pr-title="Python Requirements Update" --pr-body="$pr_body" \
       --user-reviewers="${PR_USER_REVIEWERS:-}" --team-reviewers="${PR_TEAM_REVIEWERS:-}" \
       --delete-old-pull-requests


deactivate
rm -rf "$venv"

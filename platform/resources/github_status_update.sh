#!/bin/bash
set -eu -o pipefail

venv="$WORKSPACE/github-status-env"

echo "GIT_SHA"
echo "$GIT_SHA"

pwd
ls
virtualenv --python="python3.8" "$venv" --clear --quiet
source "$venv/bin/activate"

pip install pip

cd "$WORKSPACE/testeng-ci"
pip install -r requirements/base.txt

# TODO remove this, once done testing
git checkout BOM-2450
git reset --hard origin/BOM-2450
git pull
pwd
ls
python -m jenkins.github_checks_handler

deactivate

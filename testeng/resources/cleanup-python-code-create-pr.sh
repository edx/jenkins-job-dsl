#!/bin/bash
set -e

rm -rf code_cleanup_venv
virtualenv --python=python$PYTHON_VERSION code_cleanup_venv -q
source code_cleanup_venv/bin/activate

echo "Upgrading pip..."
pip install pip==20.0.2

echo "Getting current sha..."
cd $REPO_NAME
export CURRENT_SHA=$(git rev-parse HEAD)

echo "Running cleanup..."

PACKAGESTOINSTALL=`echo $PACKAGES | tr , " "`

pip install $PACKAGESTOINSTALL

IFS=',' read -ra SCRIPTSTORUN <<< "$SCRIPTS"
for element in "${SCRIPTSTORUN[@]}"; do
  `echo $element`
done

echo "Running script to create PR..."
cd ../testeng-ci
pip install -r requirements/base.txt
python -m jenkins.cleanup_python_code --sha=$CURRENT_SHA --repo_root="../$REPO_NAME" --user_reviewers=$PR_USER_REVIEWERS --team_reviewers=$PR_TEAM_REVIEWERS --packages="$PACKAGES" --scripts="$SCRIPTS"

deactivate
cd ..
rm -rf code_cleanup_venv


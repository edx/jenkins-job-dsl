#!/bin/bash
set -exuo pipefail

# Required by click http://click.pocoo.org/5/python3/
export LC_ALL=C.UTF-8
export LANG=C.UTF-8
export GIT_AUTHOR_NAME
export GIT_AUTHOR_EMAIL
export GIT_COMMITTER_NAME
export GIT_COMMITTER_EMAIL


cd $WORKSPACE/py-opt-cli
pip install -r requirements.txt
pip install -e .
env

cd $WORKSPACE/optimizely-experiments
py-opt-cli pull

git add -A
git diff --quiet || git commit -am "history commit job # ${BUILD_ID}"
git push origin HEAD:history

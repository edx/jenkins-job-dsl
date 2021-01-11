#!/bin/bash
set -exuo pipefail

set +u
. /edx/var/jenkins/jobvenvs/virtualenv_tools.sh
# creates a venv with its location stored in variable "venvpath"
create_virtualenv --python=python3.8 --clear
. "$venvpath/bin/activate"
set -u

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

# Retry 5 times before giving up on optimizely
for i in {1..5}
do
    # Don't sleep the first time through the loop
    [ $i -gt 1 ] && sleep 300

    # Save the return-code of the pull command
    py-opt-cli pull && s=0 && break || s=$?
done
# Fail the build with the return code of the pull command if it failed after 5 retries
(exit $s)

git add -A
git diff --cached --quiet || git commit -am "history commit job # ${BUILD_ID}"
git push origin HEAD:history

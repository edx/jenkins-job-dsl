#!/bin/bash
set -exuo pipefail

set +u
. /edx/var/jenkins/jobvenvs/virtualenv_tools.sh
# creates a venv with its location stored in variable "venvpath"
create_virtualenv --python=python3.8 --clear
. "$venvpath/bin/activate"
set -u

cd $WORKSPACE/prospectus
pip install -r scripts/vendor-monitoring-requirements.txt
env

python scripts/vendor-monitoring.py\
    --algolia_auth_key ${ALGOLIA_AUTH_KEY}\
    --contentful_auth_key ${CONTENTFUL_AUTH_KEY}\
    --email ${EMAIL}

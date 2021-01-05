#!/bin/bash
set -exuo pipefail

set +u
. /edx/var/jenkins/jobvenvs/virtualenv_tools.sh
# creates a venv with its location stored in variable "venvpath"
create_virtualenv --python=python3.8 --clear
. "$venvpath/bin/activate"
set -u

cd $WORKSPACE/configuration
pip install -r util/jenkins/requirements-cloudflare.txt
env

python util/jenkins/cloudflare-hit-rate.py\
    --zone ${ZONE_ID}\
    --auth_key ${AUTH_KEY}\
    --email ${EMAIL}\
    --threshold ${THRESHOLD}

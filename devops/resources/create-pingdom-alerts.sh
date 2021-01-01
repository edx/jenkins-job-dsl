#!/bin/bash
set -exuo pipefail

set +u
. /edx/var/jenkins/jobvenvs/virtualenv_tools.sh
# creates a venv with its location stored in variable "venvpath"
create_virtualenv --python=python3.8 --clear
. "$venvpath/bin/activate"
set -u

cd $WORKSPACE/configuration
pip install -r util/pingdom/requirements.txt
env

python util/pingdom/create_pingdom_alerts.py\
    --pingdom-email ${PINGDOM_EMAIL}\
    --pingdom-password ${PINGDOM_PASSWORD}\
    --pingdom-api-key ${PINGDOM_API_KEY}\
    --alert-config-file ../${PINGDOM_ALERT_CONFIG_FILE}

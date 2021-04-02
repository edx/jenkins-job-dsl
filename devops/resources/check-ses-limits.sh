#!/bin/bash
set -exuo pipefail

set +u
. /edx/var/jenkins/jobvenvs/virtualenv_tools.sh
# creates a venv with its location stored in variable "venvpath"
create_virtualenv --python=python3.8 --clear
. "$venvpath/bin/activate"
set -u

cd $WORKSPACE/configuration
pip install -r requirements3.txt
env
export EC2_CACHE_PATH="ec2-cache"
. util/jenkins/assume-role.sh

# Assume the role that will allow call getSesLimits
assume-role ${ROLE_ARN}

python util/jenkins/check-ses-limits.py\
    --critical ${CRIT_THRESHOLD}\
    --warning ${WARN_THRESHOLD}\
    --region ${REGIONS}

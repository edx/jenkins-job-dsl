#!/bin/bash

set +u
. /edx/var/jenkins/jobvenvs/virtualenv_tools.sh
# creates a venv with its location stored in variable "venvpath"
create_virtualenv --python=python3.8 --clear
. "$venvpath/bin/activate"
set -u

cd $WORKSPACE/configuration/util/jenkins/primary_keys

pip install -r requirements.txt
. ../assume-role.sh

# Assume role for different envs
set +x
assume-role ${ROLE_ARN}
set -x

# Set RDSIGNORE if not set in job, need because we're setting -u
# Otherwise we get an error "RDSIGNORE: unbound variable"
if [[ ! -v RDSIGNORE ]]; then
    RDSIGNORE=""
fi
if [[ ! -v WHITELISTREGIONS ]]; then
    WHITELISTREGIONS=""
fi

python ./check_primary_keys.py --environment ${ENVIRONMENT} --deploy ${DEPLOYMENT} --region $AWS_DEFAULT_REGION --recipient $TO_ADDRESS --sender $FROM_ADDRESS ${RDSIGNORE} ${WHITELISTREGIONS}

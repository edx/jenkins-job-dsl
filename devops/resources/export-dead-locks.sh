#!/bin/bash

set +u
. /edx/var/jenkins/jobvenvs/virtualenv_tools.sh
# creates a venv with its location stored in variable "venvpath"
create_virtualenv --python=python3.8 --clear
. "$venvpath/bin/activate"
set -u

cd $WORKSPACE/configuration/util/jenkins/export_dead_locks

pip install -r requirements.txt
. ../assume-role.sh

# Assume the role
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

# Check if ENVIRONMENT is set to "stage"
# As we have "stg" in datadog and "stage" in the configuration
if [[ "$ENVIRONMENT" == "stage" ]]; then
  # Update ENVIRONMENT to "stg"
  ENVIRONMENT="stg"
fi

python export_dead_locks_dd.py --environment ${ENVIRONMENT} --indexname ${INDEXNAME} ${RDSIGNORE} ${WHITELISTREGIONS}

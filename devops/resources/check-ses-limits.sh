#!/bin/bash
set -exuo pipefail

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

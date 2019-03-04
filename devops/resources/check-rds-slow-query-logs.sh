#!/bin/bash

set -x 

cd $WORKSPACE/configuration

pip install -r requirements.txt
pip install click

. util/jenkins/assume-role.sh

assume-role ${ROLE_ARN}

if [[ ! -v WHITELIST ]]; then
    WHITELIST=""
fi

python util/check_rds_slow_query_logs.py --db_engine mysql ${WHITELIST}

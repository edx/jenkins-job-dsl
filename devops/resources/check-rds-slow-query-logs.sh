#!/bin/bash

set -x 

cd $WORKSPACE/configuration

. util/jenkins/assume-role.sh
assume-role ${ROLE_ARN}

cd $WORKSPACE/configuration/util/check_rds_slow_query_logs

pip install -r requirements.txt

if [[ ! -v IGNORE_OPTIONS ]]; then
    IGNORE_OPTIONS=""
fi

python check_rds_slow_query_logs.py --db_engine mysql ${IGNORE_OPTIONS}

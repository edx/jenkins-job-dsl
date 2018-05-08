#!/bin/bash
set -exuo pipefail

cd $WORKSPACE/configuration
pip install -r util/pingdom/requirements.txt
env

python util/pingdom/create_pingdom_alerts.py\
    --pingdom-email ${PINGDOM_EMAIL}\
    --pingdom-password ${PINGDOM_PASSWORD}\
    --pingdom-api-key ${PINGDOM_API_KEY}\
    --alert-config-file ../${PINGDOM_ALERT_CONFIG_FILE}

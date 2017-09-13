#!/bin/bash
set -exuo pipefail

cd $WORKSPACE/configuration
pip install -r util/jenkins/requirements-cloudflare.txt
env

python util/jenkins/cloudflare-hit-rate.py\
    --zone ${ZONE_ID}\
    --auth_key ${AUTH_KEY}\
    --email ${EMAIL}\
    --threshold ${THRESHOLD}
#!/bin/bash
set -exuo pipefail

cd $WORKSPACE/configuration
env

python util/jenkins/cloudflare-hit-rate-monitor.py\
    --zone ${ZONE_ID}\
    --auth-key ${AUTH_KEY}\
    --email ${EMAIL}\
    --threshold ${THRESHOLD}
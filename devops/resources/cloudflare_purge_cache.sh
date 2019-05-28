#!/bin/bash
#set -exuo pipefail

env
set -x

cd $WORKSPACE/configuration
pip install -r util/cloudflare/by_origin_purger/requirements.txt
pip install -r requirements.txt
. util/jenkins/assume-role.sh

assume-role ${ROLE_ARN}

aws s3 ls s3://${BUCKET} --recursive | awk '{print $4}' > targets

if ${CONFIRM_PURGE}; then
python util/cloudflare/by_origin_purger/purger.py\
    --cloudflare_zone_id ${ZONE_ID}\
    --cloudflare_api_key ${AUTH_KEY}\
    --cloudflare_site_url ${SITE}\
    --cloudflare_email ${EMAIL}\
    --origin ${ORIGIN}\
    --target_path targets --confirm
else
python util/cloudflare/by_origin_purger/purger.py\
    --cloudflare_zone_id ${ZONE_ID}\
    --cloudflare_api_key ${AUTH_KEY}\
    --cloudflare_site_url ${SITE}\
    --cloudflare_email ${EMAIL}\
    --origin ${ORIGIN}\
    --target_path targets
fi

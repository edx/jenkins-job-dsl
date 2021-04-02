#!/bin/bash

set +u
. /edx/var/jenkins/jobvenvs/virtualenv_tools.sh
# creates a venv with its location stored in variable "venvpath"
create_virtualenv --python=python3.8 --clear
. "$venvpath/bin/activate"
set -u

env
set -x

cd $WORKSPACE/configuration
pip install -r util/cloudflare/by_origin_purger/requirements.txt
pip install -r requirements.txt
. util/jenkins/assume-role.sh

assume-role ${ROLE_ARN}

aws s3 ls s3://${BUCKET} --recursive | awk '{print $4}' > targets

if [[ "${SITE}" = "https://edx.org" ]]; then
ZONE_ID=${EDX_ZONE_ID}
elif [[ "${SITE}" = "https://edx-cdn.org" ]]; then
ZONE_ID=${EDX_CDN_ZONE_ID}
elif [[ "${SITE}" = "https://edx-video.net" ]]; then
ZONE_ID=${EDX_VIDEO_ZONE_ID}
fi

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

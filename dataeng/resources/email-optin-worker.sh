#!/usr/bin/env bash

TODAY=$(date +%d)

env | sort

${EXPORTER_VENV}/bin/exporter \
   ${EXTRA_OPTIONS} ${CONFIG_PATH} ${ORG_CONFIG_PATH} \
   --org=${ORG} \
   --task=OrgEmailOptInTask \
   --output-bucket=${OUTPUT_BUCKET} \
   --output-prefix=${OUTPUT_PREFIX} \
   --django-admin=${PLATFORM_VENV}/bin/django-admin \
   --django-pythonpath=${PLATFORM_VENV}/edx-platform \
   --gpg-keys=${GPG_KEYS_PATH}

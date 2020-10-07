TODAY=$(date +%d)

env | sort

${EXPORTER_VENV}/bin/exporter \
    --org=${ORG} \
    --output-bucket=${OUTPUT_BUCKET} \
    --external-prefix=databases/${DATE:-$TODAY} \
    --django-admin=${PLATFORM_VENV}/bin/django-admin.py \
    --django-pythonpath=${PLATFORM_VENV}/edx-platform \
    --gpg-keys=${GPG_KEYS_PATH} \
    ${EXTRA_OPTIONS} ${CONFIG_PATH} ${ORG_CONFIG_PATH}

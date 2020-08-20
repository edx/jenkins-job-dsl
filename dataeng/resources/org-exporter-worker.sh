TODAY=$(date +%d)

env | sort

# Uncomment the following section only while working DENG-382
#
# Fetch the latest config:
#mkdir ${WORKSPACE}/remote-config
#aws s3 cp s3://edx-remote-config/prod/lms.yml ${WORKSPACE}/remote-config/lms.yml
#aws s3 cp s3://edx-remote-config/prod/studio.yml ${WORKSPACE}/remote-config/studio.yml

${EXPORTER_VENV}/bin/exporter \
   --org=${ORG} \
   --output-bucket=${OUTPUT_BUCKET} \
   --external-prefix=databases/${DATE:-$TODAY} \
   --django-admin=${PLATFORM_VENV}/bin/django-admin.py \
   --django-pythonpath=${PLATFORM_VENV}/edx-platform \
   --gpg-keys=${GPG_KEYS_PATH} \
   ${EXTRA_OPTIONS} ${CONFIG_PATH} ${ORG_CONFIG_PATH}

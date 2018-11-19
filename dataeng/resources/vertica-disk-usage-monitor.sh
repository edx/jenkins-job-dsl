#!/usr/bin/env bash
set -ex

if [ ! -d "/tmp/vertica-audit-venv" ]; then
  mkdir /tmp/vertica-audit-venv
  virtualenv /tmp/vertica-audit-venv
fi

. /tmp/vertica-audit-venv/bin/activate

cd vertica-audit
pip install -r requirements.txt

aws --region us-east-1 s3 cp $CONFIG_FILE_PATH ./config.yaml

python vertica-audit.py $THRESHOLD

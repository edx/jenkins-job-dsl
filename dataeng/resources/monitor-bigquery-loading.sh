#!/usr/bin/env bash

env

VENV_ROOT=$WORKSPACE/venvs
mkdir -p $VENV_ROOT

virtualenv $VENV_ROOT/analytics-tools
TOOLS_BIN=$VENV_ROOT/analytics-tools/bin
. $TOOLS_BIN/activate

pip install -r ${WORKSPACE}/analytics-tools/bigquery-monitoring/requirements.txt

PARTITION_DATE=$(date +%Y-%m-%d -d "$PARTITION_DATE")

python ${WORKSPACE}/analytics-tools/bigquery-monitoring/bigquery_monitor_event_loading.py \
    --credentials "$CREDENTIALS" \
    --date "$PARTITION_DATE"

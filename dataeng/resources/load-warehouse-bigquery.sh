#!/usr/bin/env bash

env

${WORKSPACE}/analytics-configuration/automation/run-automated-task.sh \
  LoadWarehouseBigQueryTask --local-scheduler \
  --date $(date +%Y-%m-%d -d "$TO_DATE") \
  --dataset-id $DATASET \
  --credentials $CREDENTIALS \
  --overwrite

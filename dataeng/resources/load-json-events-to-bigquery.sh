#!/usr/bin/env bash

env

${WORKSPACE}/analytics-configuration/automation/run-automated-task.sh \
  LoadEventRecordIntervalToBigQuery --local-scheduler \
  --interval $(date +%Y-%m-%d -d "$FROM_DATE")-$(date +%Y-%m-%d -d "$TO_DATE") \
  --credentials $CREDENTIALS \
  --dataset-id $DATASET \
  --warehouse-path $OUTPUT_URL \
  $EXTRA_ARGS

#!/usr/bin/env bash

env

${WORKSPACE}/analytics-configuration/automation/run-automated-task.sh \
  LoadEventsIntoWarehouseWorkflow --local-scheduler \
  --interval $(date +%Y-%m-%d -d "$FROM_DATE")-$(date +%Y-%m-%d -d "$TO_DATE") \
  --events-list-file-path $EVENTS_LIST \
  --warehouse-path $OUTPUT_URL \
  --credentials $CREDENTIALS \
  --schema $SCHEMA

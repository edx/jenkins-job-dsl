#!/usr/bin/env bash
env

${WORKSPACE}/analytics-configuration/automation/run-automated-task.sh \
  EventRecordIntervalTask --local-scheduler \
  --interval $(date +%Y-%m-%d -d "$FROM_DATE")-$(date +%Y-%m-%d -d "$TO_DATE") \
  --n-reduce-tasks $NUM_TASK_CAPACITY \
  --events-list-file-path $EVENTS_LIST \
  --warehouse-path $OUTPUT_URL \
  --PerDateTrackingEventRecordDataTask-source "$SOURCE" \
  $EVENT_RECORD_TYPE \
  $EXTRA_ARGS

#!/usr/bin/env bash

if [ -z "$NUM_REDUCE_TASKS" ]; then
    NUM_REDUCE_TASKS=$(( $NUM_TASK_CAPACITY*2 ))
fi
env

COMPLETE_OUTPUT_URL=$OUTPUT_URL/$(date +%Y-%m-%d -d "$FROM_DATE")-$(date +%Y-%m-%d -d "$TO_DATE")

${WORKSPACE}/analytics-configuration/automation/run-automated-task.sh \
  PushToVerticaEventTypeDistributionTask --local-scheduler \
  --output-root $COMPLETE_OUTPUT_URL \
  --interval $(date +%Y-%m-%d -d "$FROM_DATE")-$(date +%Y-%m-%d -d "$TO_DATE") \
  --n-reduce-tasks $NUM_REDUCE_TASKS \
  --credentials $CREDENTIALS \
  --events-list-file-path $EVENTS_LIST \
  $EXTRA_ARGS

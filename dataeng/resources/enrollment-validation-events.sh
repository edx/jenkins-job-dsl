#!/usr/bin/env bash

END_DATE=$(date +%Y-%m-%d -d "$TO_DATE")
START_DATE=$(date +%Y-%m-%d -d "$FROM_DATE")
INTERVAL=$START_DATE-$END_DATE

if [ -z "$NUM_REDUCE_TASKS" ]; then
    NUM_REDUCE_TASKS=$(( $NUM_TASK_CAPACITY + 1 ))
fi
env|sort

${WORKSPACE}/analytics-configuration/automation/run-automated-task.sh \
  EnrollmentValidationWorkflow --local-scheduler \
  --output-root $OUTPUT_ROOT/$INTERVAL \
  --interval $INTERVAL \
  --credentials $CREDENTIALS \
  --n-reduce-tasks $NUM_REDUCE_TASKS \
  $EXTRA_ARGS

pip install awscli

MAX_FILE_SIZE=`aws s3 ls $OUTPUT_ROOT/$INTERVAL/ | cut -b20-30 | grep -v PRE | sort -nr | head -1`
if [ -n "$MAX_FILE_SIZE" ] && [ "$MAX_FILE_SIZE" -gt "$FILE_THRESHOLD" ]; then echo "OVER" "THRESHOLD"; fi

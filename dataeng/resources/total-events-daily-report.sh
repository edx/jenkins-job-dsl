#!/usr/bin/env bash

if [ -z "$NUM_REDUCE_TASKS" ]; then
    NUM_REDUCE_TASKS=$(( $NUM_TASK_CAPACITY + 1 ))
fi

rm -rf venvs

env|sort
DAY_TO_REPORT_DATE_FORMATTED=$(date +%Y-%m-%d -d "$DAY_TO_REPORT")

${WORKSPACE}/analytics-configuration/automation/run-automated-task.sh \
  TotalEventsReportWorkflow --local-scheduler \
  --interval $DAY_TO_REPORT_DATE_FORMATTED \
  --n-reduce-tasks $NUM_REDUCE_TASKS \
  --report $S3_DIR/reports/$DAY_TO_REPORT_DATE_FORMATTED \
  --counts $S3_DIR/counts/$DAY_TO_REPORT_DATE_FORMATTED/ \
  $EXTRA_ARGS

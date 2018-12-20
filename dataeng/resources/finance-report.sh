#!/usr/bin/env bash

if [ -z "$NUM_REDUCE_TASKS" ]; then
    NUM_REDUCE_TASKS=$(( $NUM_TASK_CAPACITY*2 ))
fi

${WORKSPACE}/analytics-configuration/automation/run-automated-task.sh \
  BuildFinancialReportsTask --local-scheduler \
    --import-date $(date +%Y-%m-%d -d "$TO_DATE") \
    --schema $OUTPUT_SCHEMA \
    --n-reduce-tasks $NUM_REDUCE_TASKS \
    $EXTRA_ARGS


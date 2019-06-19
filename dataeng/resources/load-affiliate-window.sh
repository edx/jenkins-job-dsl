#!/usr/bin/env bash

if [ -z "$NUM_REDUCE_TASKS" ]; then
    NUM_REDUCE_TASKS=$(( $NUM_TASK_CAPACITY*2 ))
fi

${WORKSPACE}/analytics-configuration/automation/run-automated-task.sh \
  LoadFeesToWarehouse --local-scheduler \
    --run-date $(date +%Y-%m-%d -d "$TO_DATE") \
    --schema $OUTPUT_SCHEMA \
    $EXTRA_ARGS

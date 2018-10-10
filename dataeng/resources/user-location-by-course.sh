#!/usr/bin/env bash

END_DATE=$(date +%Y-%m-%d -d "$TO_DATE")

if [ -z "$NUM_REDUCE_TASKS" ]; then
    NUM_REDUCE_TASKS=$(( $NUM_TASK_CAPACITY * 2 ))
fi

env

${WORKSPACE}/analytics-configuration/automation/run-automated-task.sh \
  ImportCountryWorkflow --local-scheduler \
  --interval-end $END_DATE \
  --n-reduce-tasks $NUM_REDUCE_TASKS \
  --overwrite

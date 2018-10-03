#!/usr/bin/env bash

if [ "x$NUM_REDUCE_TASKS" = "x" ]; then
    NUM_REDUCE_TASKS=$(( $NUM_TASK_CAPACITY * 2 ))
fi

analytics-configuration/automation/run-automated-task.sh \
  ModuleEngagementWorkflowTask --local-scheduler \
     --date $(date +%Y-%m-%d -d "$TO_DATE") \
     --indexing-tasks 5 \
     --throttle 0.5 \
     --n-reduce-tasks $NUM_REDUCE_TASKS \
     $ADDITIONAL_TASK_ARGS

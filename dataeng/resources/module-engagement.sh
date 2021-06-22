#!/usr/bin/env bash

if [ -z "$NUM_REDUCE_TASKS" ]; then
    NUM_CAPACITY=$(( $NUM_TASK_CAPACITY + $ON_DEMAND_CAPACITY ))
    NUM_REDUCE_TASKS=$(( $NUM_CAPACITY*2 ))
fi

${WORKSPACE}/analytics-configuration/automation/run-automated-task.sh \
  ModuleEngagementWorkflowTask --local-scheduler \
     --date $(date +%Y-%m-%d -d "$TO_DATE") \
     --throttle 0.5 \
     --n-reduce-tasks $NUM_REDUCE_TASKS \
     $EXTRA_ARGS


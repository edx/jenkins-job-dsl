#!/usr/bin/env bash

if [ "x$NUM_REDUCE_TASKS" = "x" ]; then
    NUM_REDUCE_TASKS=$(( $NUM_TASK_CAPACITY * 2 ))
fi

env

${WORKSPACE}/analytics-configuration/automation/run-automated-task.sh \
 LoadWarehouseWorkflow --local-scheduler \
 --n-reduce-tasks $NUM_REDUCE_TASKS \
 --date $(date +%Y-%m-%d -d "$TO_DATE") \
 --schema $SCHEMA \
 --credentials $CREDENTIALS \
 --marker-schema $MARKER_SCHEMA \
 --overwrite \
 $EXTRA_ARGS


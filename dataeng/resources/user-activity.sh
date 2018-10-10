#!/usr/bin/env bash

if [ -z "$NUM_REDUCE_TASKS" ]; then
    NUM_REDUCE_TASKS=$(( $NUM_TASK_CAPACITY*2 ))
fi

env | sort

${WORKSPACE}/analytics-configuration/automation/run-automated-task.sh \
  InsertToMysqlCourseActivityTask --local-scheduler \
  --end-date $(date +%Y-%m-%d -d "$TO_DATE") \
  --weeks 52 \
  --n-reduce-tasks $NUM_REDUCE_TASKS \
  --overwrite-mysql

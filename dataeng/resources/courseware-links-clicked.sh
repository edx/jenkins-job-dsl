#!/usr/bin/env bash

if [ -z "$NUM_REDUCE_TASKS" ]; then
    NUM_REDUCE_TASKS=$(( $NUM_TASK_CAPACITY * 2 ))
fi

${WORKSPACE}/analytics-configuration/automation/run-automated-task.sh \
  PushToVerticaLMSCoursewareLinkClickedTask --local-scheduler \
     --interval $(date +%Y-%m-%d -d "$FROM_DATE")-$(date +%Y-%m-%d -d "$TO_DATE") \
     --output-root s3://edx-analytics-scratch/output/prod/links_clicked/ \
     --n-reduce-tasks $NUM_REDUCE_TASKS \
     $EXTRA_ARGS

#!/usr/bin/env bash

if [ -z "$NUM_REDUCE_TASKS" ]; then
    NUM_REDUCE_TASKS=$(( $NUM_TASK_CAPACITY*2 ))
fi

env

${WORKSPACE}/analytics-configuration/automation/run-automated-task.sh \
    BuildProgramReportsTask --local-scheduler \
    --vertica-warehouse-name warehouse \
    --vertica-credentials $VERTICA_CREDENTIALS \
    --n-reduce-tasks $NUM_REDUCE_TASKS \
    --overwrite \
    --output-root $OUTPUT_ROOT \
    $EXTRA_ARGS


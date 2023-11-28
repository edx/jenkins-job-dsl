#!/usr/bin/env bash

env

DAY_OF_MONTH=$(date +%d)
OUTPUT_URL=$BASE_OUTPUT_URL/$DAY_OF_MONTH/$OUTPUT_DIR
NUM_CAPACITY=$(( $NUM_TASK_CAPACITY + $ON_DEMAND_CAPACITY ))
NUM_MAPPERS=$(( ($NUM_CAPACITY + 1) * 2 ))
NUM_REDUCE_TASKS=$(( $NUM_CAPACITY + 1 ))

${WORKSPACE}/analytics-configuration/automation/run-automated-task.sh \
  StudentModulePerCourseAfterImportWorkflow --local-scheduler \
  --credentials $CREDENTIALS \
  --dump-root $OUTPUT_URL/raw/ \
  --output-root $OUTPUT_URL/ \
  --delete-output-root \
  --output-suffix $OUTPUT_SUFFIX \
  --num-mappers $NUM_MAPPERS \
  --n-reduce-tasks $NUM_REDUCE_TASKS \
  --verbose \
  $EXTRA_ARGS


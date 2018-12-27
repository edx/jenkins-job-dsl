#!/usr/bin/env bash

env

DAY_OF_MONTH=$(date +%d)
OUTPUT_URL=$BASE_OUTPUT_URL/$DAY_OF_MONTH/$OUTPUT_DIR
NUM_REDUCE_TASKS=$(( $NUM_TASK_CAPACITY + 1 ))

${WORKSPACE}/analytics-configuration/automation/run-automated-task.sh \
  StudentModulePerCourseAfterImportWorkflow --local-scheduler \
  --credentials $CREDENTIALS \
  --dump-root $OUTPUT_URL/raw/ \
  --output-root $OUTPUT_URL/ \
  --delete-output-root \
  --output-suffix $OUTPUT_SUFFIX \
  --num-mappers $(( ($NUM_TASK_CAPACITY + 1) * 2 )) \
  --n-reduce-tasks $NUM_REDUCE_TASKS \
  --verbose \
  $EXTRA_ARGS


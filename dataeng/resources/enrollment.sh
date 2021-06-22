#!/usr/bin/env bash

if [ -z "$NUM_REDUCE_TASKS" ]; then
    NUM_CAPACITY=$(( $NUM_TASK_CAPACITY + $ON_DEMAND_CAPACITY ))
    NUM_REDUCE_TASKS=$(( $NUM_CAPACITY*2 ))
fi

env

# Interpolate the TO_DATE now so that the downstream job is guaranteed to use
# the same exact date as this job. Otherwise, if this job runs over a date
# boundary, the downstream job would re-interpolate the value of 'yesterday' on
# a different date.
INTERPOLATED_TO_DATE="$(date +%Y-%m-%d -d "$TO_DATE")"
echo "TO_DATE=${INTERPOLATED_TO_DATE}" > "${WORKSPACE}/downstream.properties"

${WORKSPACE}/analytics-configuration/automation/run-automated-task.sh \
  ImportEnrollmentsIntoMysql --local-scheduler \
  --interval $(date +%Y-%m-%d -d "$FROM_DATE")-$(date +%Y-%m-%d -d "$TO_DATE") \
  --n-reduce-tasks $NUM_REDUCE_TASKS \
  --overwrite-mysql \
  --EnrollmentByGenderMysqlTask-use-temp-table-for-overwrite \
  $EXTRA_ARGS


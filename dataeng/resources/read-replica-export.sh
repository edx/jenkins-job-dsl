#!/usr/bin/env bash

set -ex

env

# Cleanup downstream properties file first:
DOWNSTREAM_PROPERTIES_FILE="${WORKSPACE}/downstream.properties"
rm "${DOWNSTREAM_PROPERTIES_FILE}" || true

# Keep track of the start time to feed into downstream validation scripts.
echo "SQOOP_START_TIME=$(date --utc --iso=minutes)" >> "${DOWNSTREAM_PROPERTIES_FILE}"

# Interpolate the RUN_DATE now so that the downstream job is guaranteed to use
# the same exact date as this job. Otherwise, if this job runs over a date
# boundary, the downstream job would re-interpolate the value of 'yesterday' on
# a different date.
INTERPOLATED_RUN_DATE="$(date +%Y-%m-%d -d "$TO_DATE")"
echo "RUN_DATE=${INTERPOLATED_RUN_DATE}" >> "${DOWNSTREAM_PROPERTIES_FILE}"

${WORKSPACE}/analytics-configuration/automation/run-automated-task.sh \
 ExportMysqlDatabaseToS3Task --local-scheduler \
 --date ${INTERPOLATED_RUN_DATE} \
 --db-credentials $DB_CREDENTIALS \
 --database $DATABASE \
 --overwrite \
 ${EXCLUDE_FIELD} \
 ${INCLUDE} \
 ${EXCLUDE} \
 ${EXTRA_ARGS}

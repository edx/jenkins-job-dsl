#!/usr/bin/env bash

env

${WORKSPACE}/analytics-configuration/automation/run-automated-task.sh \
 ImportMysqlToVerticaTask --local-scheduler \
 --date $(date +%Y-%m-%d -d "$TO_DATE") \
 --schema $SCHEMA \
 --credentials $CREDENTIALS \
 --db-credentials $DB_CREDENTIALS \
 --database $DATABASE \
 --marker-schema $MARKER_SCHEMA \
 --overwrite \
 ${EXCLUDE_FIELD} \
 ${EXCLUDE}

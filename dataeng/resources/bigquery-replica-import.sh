#!/usr/bin/env bash

env

${WORKSPACE}/analytics-configuration/automation/run-automated-task.sh \
 ImportMysqlDatabaseToBigQueryDatasetTask --local-scheduler \
 --date $(date +%Y-%m-%d -d "$TO_DATE") \
 --dataset-id $DATASET \
 --credentials $CREDENTIALS \
 --db-credentials $DB_CREDENTIALS \
 --database $DATABASE \
 ${EXCLUDE} \
 ${EXCLUDE_FIELD} \
 --overwrite


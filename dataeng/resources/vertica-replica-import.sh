#!/usr/bin/env bash

env

${WORKSPACE}/analytics-configuration/automation/run-automated-task.sh \
 ImportMysqlDatabaseFromS3ToVerticaSchemaTask --local-scheduler \
 --date $(date +%Y-%m-%d -d "$RUN_DATE") \
 --schema $SCHEMA \
 --credentials $CREDENTIALS \
 --database $DATABASE \
 --marker-schema $MARKER_SCHEMA \
 --overwrite \
 ${INCLUDE} \
 ${EXCLUDE} \
 ${EXTRA_ARGS}

#!/usr/bin/env bash

env

${WORKSPACE}/analytics-configuration/automation/run-automated-task.sh \
 VerticaSchemaToBigQueryTask --local-scheduler \
 --vertica-schema-name $SCHEMA \
 --vertica-credentials $VERTICA_CREDENTIALS \
 --gcp-credentials $GCP_CREDENTIALS \
 --date $(date +%Y-%m-%d -d "$RUN_DATE") \
 ${OVERWRITE} \
 ${EXCLUDE} \
 ${EXTRA_ARGS}


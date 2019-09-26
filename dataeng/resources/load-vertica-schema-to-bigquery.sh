#!/usr/bin/env bash

env

${WORKSPACE}/analytics-configuration/automation/run-automated-task.sh \
 LoadVerticaSchemaFromS3ToBigQueryTask --local-scheduler \
 --date $(date +%Y-%m-%d -d "$RUN_DATE") \
 ${OVERWRITE} \
 --gcp-credentials $GCP_CREDENTIALS \
 --vertica-schema-name $VERTICA_SCHEMA_NAME \
 --vertica-credentials $VERTICA_CREDENTIALS \
 ${EXCLUDE} \
 ${EXTRA_ARGS}
